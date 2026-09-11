with open("kasir-android/app-store/src/main/java/com/kasirinaja/store/ui/MainScreen.kt", "r") as f:
    data = f.read()

data = data.replace("ReportsViewModel.Factory(database.transactionDao())", "ReportsViewModel.Factory(database.transactionDao(), productRepository)")

with open("kasir-android/app-store/src/main/java/com/kasirinaja/store/ui/MainScreen.kt", "w") as f:
    f.write(data)
