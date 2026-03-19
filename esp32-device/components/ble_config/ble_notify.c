#include "ble_notify.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"

#include <string.h>

static QueueHandle_t notify_queue;
static esp_gatt_if_t notify_gatts_if = 0;
static uint16_t notify_conn_id = 0;
static uint16_t notify_char_handle = 0;
static bool notify_enabled = false;

static void ble_notify_task(void *arg)
{
    ble_notify_msg_t msg;

    while (1)
    {
        if (xQueueReceive(notify_queue, &msg, portMAX_DELAY))
        {
            if (!notify_enabled)
                continue;

            // Send raw bytes (event + payload)
            uint8_t buf[1 + BLE_NOTIFY_MAX_PAYLOAD];
            buf[0] = (uint8_t)msg.event;
            memcpy(buf + 1, msg.payload, msg.payload_len);

            esp_ble_gatts_send_indicate(
                notify_gatts_if,
                notify_conn_id,
                notify_char_handle,
                1 + msg.payload_len,
                buf,
                false);
        }
    }
}

void ble_notify_init(void)
{
    notify_queue = xQueueCreate(10, sizeof(ble_notify_msg_t));
    xTaskCreate(ble_notify_task, "ble_notify", 4096, NULL, 5, NULL);
}

void ble_notify_set_connection(esp_gatt_if_t gatts_if, uint16_t conn_id)
{
    notify_gatts_if = gatts_if;
    notify_conn_id = conn_id;
}

void ble_notify_set_enabled(bool enabled)
{
    notify_enabled = enabled;
}

void ble_notify_set_char_handle(uint16_t handle)
{
    notify_char_handle = handle;
}

bool ble_notify_send_event(const ble_notify_msg_t *msg)
{
    if (!notify_queue)
        return false;
    return xQueueSend(notify_queue, msg, pdMS_TO_TICKS(100)) == pdPASS;
}