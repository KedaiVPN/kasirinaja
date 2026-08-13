import re

with open('kasir-android/app-store/src/main/java/com/kasirinaja/store/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('val formatOptions = listOf("PDF", "XLS")', 'val formatOptions = listOf("PDF", "XLSX")')
content = content.replace('var selectedFormat by remember { mutableStateOf("XLS") }', 'var selectedFormat by remember { mutableStateOf("PDF") }')

with open('kasir-android/app-store/src/main/java/com/kasirinaja/store/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(content)
