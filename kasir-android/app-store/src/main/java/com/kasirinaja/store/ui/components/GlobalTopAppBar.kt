package com.kasirinaja.store.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTopAppBar(
    title: String,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    // We get the photo URL locally inside the component to avoid passing it around everywhere
    val userPhotoUrl = tokenManager.getPhotoUrl()

    CenterAlignedTopAppBar(
        modifier = Modifier.shadow(elevation = 4.dp),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO: Open drawer/menu */ }) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.padding(end = 8.dp).size(36.dp)
                ) {
                    if (!userPhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${RetrofitClient.IMAGE_BASE_URL}${userPhotoUrl}")
                                .crossfade(true)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            contentDescription = "Profile Menu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                MaterialTheme(
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

                        Divider(
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
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
