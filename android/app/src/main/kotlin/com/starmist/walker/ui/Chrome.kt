package com.starmist.walker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Page numbers used by the bar: today 0, stats 1, settings 2, collection 3. */
@Composable
fun AppBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Vintage.parchmentDark.copy(alpha = 0.92f)) {
        NavigationBarItem(selected == 0, { onSelect(0) }, { Icon(Icons.Filled.Home, contentDescription = null) }, label = { Text("今日") })
        NavigationBarItem(selected == 3, { onSelect(3) }, { Icon(Icons.Filled.Place, contentDescription = null) }, label = { Text("收藏") })
        NavigationBarItem(selected == 1, { onSelect(1) }, { Icon(Icons.Filled.DateRange, contentDescription = null) }, label = { Text("統計") })
        NavigationBarItem(selected == 2, { onSelect(2) }, { Icon(Icons.Filled.Settings, contentDescription = null) }, label = { Text("設定") })
    }
}
