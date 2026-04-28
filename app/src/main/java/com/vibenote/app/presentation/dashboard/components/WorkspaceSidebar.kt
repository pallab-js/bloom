package com.vibenote.app.presentation.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibenote.app.core.theme.VibeColors
import com.vibenote.app.domain.model.Folder

@Composable
fun WorkspaceSidebar(
    folders: List<Folder>,
    tags: List<String>,
    onFolderClick: (Folder?) -> Unit,
    onTagClick: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = VibeColors.Dark.surfaceDark,
        drawerContentColor = VibeColors.Dark.textPrimaryDark
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Workspace",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        Divider(color = VibeColors.Dark.borderStandardDark)
        
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibeColors.Dark.brandGreen,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                SidebarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = "All Notes",
                    onClick = { onFolderClick(null) }
                )
            }
            items(folders) { folder ->
                SidebarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = folder.name,
                    onClick = { onFolderClick(folder) }
                )
            }
            
            item {
                Spacer(Modifier.height(16.dp))
                Divider(color = VibeColors.Dark.borderStandardDark)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibeColors.Dark.brandGreen,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(tags) { tag ->
                SidebarItem(
                    icon = { Icon(Icons.Default.Tag, contentDescription = null) },
                    label = tag,
                    onClick = { onTagClick(tag) }
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
