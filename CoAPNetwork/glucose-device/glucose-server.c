#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "sys/etimer.h"
#include "dev/leds.h"
#include "os/dev/serial-line.h"
#include "parameters.h"

#include "node-id.h"
#include "net/ipv6/simple-udp.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "routing/routing.h"

#include "coap-engine.h"
#include "coap-blocking-api.h"

#define SERVER_EP "coap://[fd00::1]:5683"
#define CONN_TRY_INTERVAL 1
#define REG_TRY_INTERVAL 1


#define SENSOR_TYPE "glucose_sensor"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

PROCESS(glucose_server, "Glucose sensor Server");
AUTOSTART_PROCESSES(&glucose_server);

//*************************** GLOBAL VARIABLES *****************************//
char* service_url = "/registration";

static bool connected = false;
static bool registered = false;

static struct etimer wait_connectivity;
static struct etimer wait_registration;
static struct etimer simulation;

int sampling_rate;

extern coap_resource_t res_glucose;
extern coap_resource_t res_alarm;

//*************************** UTILITY FUNCTIONS *****************************//
static void check_connection()
{
    if (!NETSTACK_ROUTING.node_is_reachable())
    {
        LOG_WARN("BR not reachable\n");
        etimer_reset(&wait_connectivity);
    }
    else
    {
        LOG_INFO("BR reachable\n");
        leds_set(LEDS_NUM_TO_MASK(LEDS_YELLOW));
        connected = true;
    }
}


void client_chunk_handler(coap_message_t *response)
{
    const uint8_t* chunk;

    if (response == NULL)
    {
        LOG_WARN("Request timed out\n");
        etimer_set(&wait_registration, CLOCK_SECOND * REG_TRY_INTERVAL);
        return;
    }
    
    int len = coap_get_payload(response, &chunk);
    
    if(strncmp((char*)chunk, "Registration, Success!", len) == 0){
        registered = true;
        leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
    }
    else
        etimer_set(&wait_registration, CLOCK_SECOND * REG_TRY_INTERVAL);
}

//*************************** MAIN THREAD *****************************//
PROCESS_THREAD(glucose_server, ev, data)
{
    PROCESS_BEGIN();

    static coap_endpoint_t server_ep;
    static coap_message_t request[1]; // This way the packet can be treated as pointer as usual

    

    leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
    etimer_set(&wait_connectivity, CLOCK_SECOND * CONN_TRY_INTERVAL);

    
    while (!connected) {
        PROCESS_WAIT_UNTIL(etimer_expired(&wait_connectivity));
        check_connection();
    }
    printf("CONNECTED\n");
    
    // Registration
    LOG_INFO("Sending registration message\n");
    coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);
    coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
    coap_set_header_uri_path(request, service_url);
    coap_set_payload(request, (uint8_t*) SENSOR_TYPE, sizeof(SENSOR_TYPE) - 1);

    while (!registered) {
        COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);
        // wait for the timer to expire
        PROCESS_WAIT_UNTIL(etimer_expired(&wait_registration));
    }

    printf("REGISTERED\n");
    printf("Starting glucose server\n");

    // RESOURCES ACTIVATION
    coap_activate_resource(&res_glucose, "glucose");
    coap_activate_resource(&res_alarm, "alarm");

    // SIMULATION
    etimer_set(&simulation, CLOCK_SECOND * sampling_rate);
    
    while (1) {
        PROCESS_WAIT_EVENT();
        
        if (ev == PROCESS_EVENT_TIMER && data == &simulation) {
            res_glucose.trigger();
            etimer_set(&simulation, CLOCK_SECOND * sampling_rate);
        } else if (ev == serial_line_event_message && data != NULL){
            //Specify via servial line the sampling rate
            sampling_rate = atol((const char*)data);
            printf("Sampling rate: %d s\n", sampling_rate);
        }
    }
    
    PROCESS_END();
}