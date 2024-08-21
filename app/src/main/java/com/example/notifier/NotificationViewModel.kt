package com.example.notifier

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class NotificationViewModel : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications

    fun addNotification(
        packageName: String?,
        title: String?,
        text: String?
    ) {
        val newNotification = NotificationData(packageName, title, text)
        _notifications.value = _notifications.value + newNotification
    }
}
