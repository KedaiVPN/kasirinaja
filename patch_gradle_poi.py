import re

with open('kasir-android/app-store/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('implementation("net.sourceforge.jexcelapi:jxl:2.6.12")', 'implementation("org.apache.poi:poi-ooxml:5.2.3")')

with open('kasir-android/app-store/build.gradle.kts', 'w') as f:
    f.write(content)
