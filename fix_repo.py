with open("kasir-android/app-store/src/main/java/com/kasirinaja/store/ui/viewmodels/ReportsViewModel.kt", "r") as f:
    data = f.read()

import_str = "import com.kasirinaja.store.data.repository.ProductRepository\n"
if import_str not in data:
    data = data.replace("import com.kasirinaja.core.network.StockReportDto", "import com.kasirinaja.core.network.StockReportDto\nimport com.kasirinaja.store.data.repository.ProductRepository")

data = data.replace("class ReportsViewModel(\n    private val transactionDao: TransactionDao", "class ReportsViewModel(\n    private val transactionDao: TransactionDao,\n    private val productRepository: ProductRepository")
data = data.replace("class Factory(\n        private val transactionDao: TransactionDao", "class Factory(\n        private val transactionDao: TransactionDao,\n        private val productRepository: ProductRepository")
data = data.replace("return ReportsViewModel(transactionDao) as T", "return ReportsViewModel(transactionDao, productRepository) as T")

search_str = """                if (response.isSuccessful) {
                    fetchStockReport() // Refresh
                    onResult(true, "Stok berhasil ditambahkan")"""
replace_str = """                if (response.isSuccessful) {
                    productRepository.syncStoreProducts() // Force local DB sync immediately
                    fetchStockReport() // Refresh remote report
                    onResult(true, "Stok berhasil ditambahkan")"""
data = data.replace(search_str, replace_str)

with open("kasir-android/app-store/src/main/java/com/kasirinaja/store/ui/viewmodels/ReportsViewModel.kt", "w") as f:
    f.write(data)
