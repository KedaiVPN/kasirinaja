import re

def main():
    file_path = "kasir-android/app-store/src/main/java/com/kasirinaja/store/ui/components/GlobalTopAppBar.kt"
    with open(file_path, 'r') as f:
        content = f.read()

    # Add shapes import
    if "import androidx.compose.foundation.shape.RoundedCornerShape" not in content:
        content = content.replace("import androidx.compose.foundation.shape.CircleShape", "import androidx.compose.foundation.shape.CircleShape\nimport androidx.compose.foundation.shape.RoundedCornerShape")

    # Modernize Dropdown Menu
    old_dropdown = """                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Profil") },
                        leadingIcon = {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = "Profil")
                        },
                        onClick = {
                            showMenu = false
                            onNavigateToEditProfile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            showMenu = false
                            onLogout()
                        }
                    )
                }"""

    new_dropdown = """                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                ) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Profil",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.AccountCircle,
                                    contentDescription = "Profil",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToEditProfile()
                            },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Logout",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Logout,
                                    contentDescription = "Logout",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onLogout()
                            },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }"""

    content = content.replace(old_dropdown, new_dropdown)

    # We also need PaddingValues
    if "import androidx.compose.foundation.layout.PaddingValues" not in content:
        content = content.replace("import androidx.compose.foundation.layout.padding", "import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.PaddingValues")

    # We also need background
    if "import androidx.compose.foundation.background" not in content:
        content = content.replace("import androidx.compose.foundation.border", "import androidx.compose.foundation.border\nimport androidx.compose.foundation.background")


    with open(file_path, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    main()
