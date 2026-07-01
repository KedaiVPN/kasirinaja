package com.kasirinaja.store.ui.viewmodels
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.local.LocalTransactionEntity
import com.kasirinaja.store.data.local.LocalTransactionItemEntity
import java.util.UUID
import com.kasirinaja.store.data.local.ProductEntity
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.kasirinaja.store.data.repository.TransactionRepository
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit
import com.kasirinaja.store.worker.TransactionSyncWorker


data class CartItem(
    val product: ProductEntity,
    var quantity: Int
)


class ScanViewModel(
    val repository: ProductRepository,
    private val transactionRepository: TransactionRepository? = null,
    private val workManager: WorkManager? = null
) : ViewModel() {

    val products: Flow<List<ProductEntity>> = repository.allProducts

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private var toneGenerator: ToneGenerator? = null

    // To prevent rapid scanning of the same barcode in a single frame sweep
    private var lastScannedBarcode: String? = null
    private var lastScanTime: Long = 0

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            // Handle exception if ToneGenerator fails to initialize
        }
    }

    fun onBarcodeScanned(barcode: String) {
        val trimmedBarcode = barcode.trim()
        val currentTime = System.currentTimeMillis()
        if (trimmedBarcode == lastScannedBarcode && (currentTime - lastScanTime) < 2000) {
            return // Debounce rapid same-barcode scans
        }
        lastScannedBarcode = trimmedBarcode
        lastScanTime = currentTime

        viewModelScope.launch {
            val product = repository.getProductByBarcode(trimmedBarcode)
            if (product != null) {
                addProductToCart(product)
            } else {
                playErrorSound()
                _toastMessage.emit("Produk tidak ditemukan: $trimmedBarcode")
            }
        }
    }

    fun addProductToCart(product: ProductEntity) {
        // Play sound
        playBeepSound()

        // Add to cart or increment
        val currentCart = _cartItems.value.toMutableList()
        val existingItemIndex = currentCart.indexOfFirst { it.product.id == product.id }

        if (existingItemIndex != -1) {
            val existingItem = currentCart[existingItemIndex]
            currentCart[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            currentCart.add(CartItem(product, 1))
        }

        _cartItems.value = currentCart
    }

    fun incrementQuantity(product: ProductEntity) {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val item = currentCart[index]
            currentCart[index] = item.copy(quantity = item.quantity + 1)
            _cartItems.value = currentCart
        }
    }

    fun decrementQuantity(product: ProductEntity) {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val item = currentCart[index]
            if (item.quantity > 1) {
                currentCart[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentCart.removeAt(index)
            }
            _cartItems.value = currentCart
        }
    }

    fun removeProduct(product: ProductEntity) {
        val currentCart = _cartItems.value.toMutableList()
        currentCart.removeAll { it.product.id == product.id }
        _cartItems.value = currentCart
    }

    fun saveTransaction(paidAmount: Double, changeAmount: Double, storeId: String, cashierId: String, onTransactionSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            val items = _cartItems.value
            if (items.isEmpty()) return@launch

            val transactionId = UUID.randomUUID().toString()
            val totalAmount = getTotalAmount()

            val transaction = LocalTransactionEntity(
                id = transactionId,
                storeId = storeId,
                cashierId = cashierId,
                invoiceNumber = "INV-${System.currentTimeMillis()}",
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                changeAmount = changeAmount,
                paymentMethod = "CASH",
                transactionTime = System.currentTimeMillis(),
                syncStatus = "pending",
                deviceId = "device_1"
            )

            val transactionItems = items.map { cartItem ->
                LocalTransactionItemEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = transactionId,
                    storeProductId = cartItem.product.id,
                    masterProductId = cartItem.product.id, // Fallback if missing
                    barcode = cartItem.product.barcode ?: "",
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    buyPrice = cartItem.product.buyPrice.toDoubleOrNull() ?: 0.0,
                    sellPrice = cartItem.product.sellPrice.toDoubleOrNull() ?: 0.0,
                    subtotal = (cartItem.product.sellPrice.toDoubleOrNull() ?: 0.0) * cartItem.quantity
                )
            }

            _toastMessage.emit("Menyimpan transaksi...")
            transactionRepository?.saveTransactionLocally(transaction, transactionItems)

            // Enqueue worker to sync immediately with network constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, // Mulai dari 30 detik untuk percobaan ulang
                    TimeUnit.SECONDS
                )
                .build()

            workManager?.enqueueUniqueWork(
                "TransactionSyncWork",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            _toastMessage.emit("Transaksi berhasil disimpan secara lokal. Sinkronisasi berjalan di latar belakang.")
            clearCart()
            onTransactionSaved(transactionId)
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getTotalAmount(): Double {
        return _cartItems.value.sumOf {
            val price = it.product.sellPrice.toDoubleOrNull() ?: 0.0
            price * it.quantity
        }
    }

    private fun playBeepSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
    }

    private fun playErrorSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 400)
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
        toneGenerator = null
    }
}
