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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val product: ProductEntity,
    var quantity: Int
)

class ScanViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private var toneGenerator: ToneGenerator? = null

    // To prevent rapid scanning of the same barcode in a single frame sweep
    private var lastScannedBarcode: String? = null
    private var lastScanTime: Long = 0

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            // Handle exception if ToneGenerator fails to initialize
        }
    }

    fun onBarcodeScanned(barcode: String) {
        val currentTime = System.currentTimeMillis()
        if (barcode == lastScannedBarcode && (currentTime - lastScanTime) < 1500) {
            return // Debounce rapid same-barcode scans
        }
        lastScannedBarcode = barcode
        lastScanTime = currentTime

        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
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
        }
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

    fun saveTransaction(paidAmount: Double, changeAmount: Double, storeId: String = "dummy_store", cashierId: String = "dummy_cashier") {
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
                syncStatus = "PENDING",
                deviceId = "device_1"
            )

            val transactionItems = items.map { cartItem ->
                LocalTransactionItemEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = transactionId,
                    storeProductId = cartItem.product.id,
                    masterProductId = cartItem.product.id, // Assuming same for local dummy
                    barcode = cartItem.product.barcode ?: "",
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    buyPrice = cartItem.product.buyPrice.toDoubleOrNull() ?: 0.0,
                    sellPrice = cartItem.product.sellPrice.toDoubleOrNull() ?: 0.0,
                    subtotal = (cartItem.product.sellPrice.toDoubleOrNull() ?: 0.0) * cartItem.quantity
                )
            }

            repository.saveTransaction(transaction, transactionItems)
            clearCart()
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
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
        toneGenerator = null
    }
}
