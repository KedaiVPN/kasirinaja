<<<<<<< SEARCH
            try {
                RetrofitClient.adminApi.approveProduct(id)
                _actionState.value = "Produk berhasil di-approve"
                loadPendingProducts()
            } catch (e: Exception) {
=======
            try {
                val response = RetrofitClient.adminApi.approveProduct(id)
                if (response.isSuccessful) {
                    _actionState.value = "Produk berhasil di-approve"
                    loadPendingProducts()
                } else {
                    _actionState.value = "Gagal approve produk: ${response.code()}"
                }
            } catch (e: Exception) {
>>>>>>> REPLACE
<<<<<<< SEARCH
            try {
                RetrofitClient.adminApi.rejectProduct(id)
                _actionState.value = "Produk berhasil di-reject"
                loadPendingProducts()
            } catch (e: Exception) {
=======
            try {
                val response = RetrofitClient.adminApi.rejectProduct(id)
                if (response.isSuccessful) {
                    _actionState.value = "Produk berhasil di-reject"
                    loadPendingProducts()
                } else {
                    _actionState.value = "Gagal reject produk: ${response.code()}"
                }
            } catch (e: Exception) {
>>>>>>> REPLACE
