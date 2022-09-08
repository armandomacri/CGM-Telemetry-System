#include <stdlib.h>
#include <time.h>
#include <string.h>

#include "contiki.h"
#include "node-id.h"
#include "coap-engine.h"
#include "parameters.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

/**************** RESOURCES **********************/

#define VARIATION 1
// mg/dL

static int glucose = 90;
int LOWER_BOUND_GLU = 100;
int UPPER_BOUND_GLU = 125;

int sampling_rate = 8;

/**************** REST: Glucose **********************/
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

EVENT_RESOURCE(res_glucose,
               "title=\"Glucose sensor\";rt=\"Glucose\";obs",
               res_get_handler,
               res_put_handler,
               res_put_handler,
               NULL,
               res_event_handler);

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    LOG_INFO("Handling glucose get request...\n");

    unsigned int accept = -1;
    coap_get_header_accept(request, &accept);

    if(accept == -1 || accept == APPLICATION_JSON)
    {

        /*
        // IF TOO HOT OR TOO COLD SEND A WARNING
        if (glucose > LOWER_BOUND_GLU && glucose <= UPPER_BOUND_GLU)
        {
            LOG_INFO("Glucose level lower higher than normal.\n");
        }
        else if (glucose > UPPER_BOUND_GLU)
        {
            LOG_INFO("Glucose level too high\n");
        }
        else
        {
;
        }
        */

        //PREPARE THE BUFFER
        snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"node_id\":%d,\"glucose\":%d,\"timestamp\":%lu}", node_id, glucose, clock_seconds());
        int length = strlen((char*)buffer);

        printf("%s\n", buffer);

        // COAP FUNCTIONS
        coap_set_header_content_format(response, APPLICATION_JSON);
        coap_set_header_etag(response, (uint8_t *)&length, 1);
        coap_set_payload(response, buffer, length);
    }
    else
    {
		coap_set_status_code(response, NOT_ACCEPTABLE_4_06);
        sprintf((char *)buffer, "Supported content-types:application/json");
	    coap_set_payload(response, buffer, strlen((char*)buffer));
	}
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    LOG_INFO("Handling glucose put request...\n");

    if(request == NULL){
		LOG_INFO("[HUM]: Empty request\n");
		return;
	}

    size_t len = 0;
    const uint8_t* payload = NULL;
    bool success = true;
    if((len = coap_get_payload(request, &payload)))
    {
        //char* msg = (char*)malloc((strlen((char*)payload))*sizeof(char));
        //strcpy(msg, (char*)payload, len);

        int new_value = atoi((char*)payload);
	
		LOG_INFO("Received the message: %s", request);
		//adapting the color of the led to the state of the fruit
		//ethylene_level = atof(data);
		LOG_INFO("New sampling rate: %d", new_value);

        
    }

    if(!success)
        coap_set_status_code(response, BAD_REQUEST_4_00);
}

static void res_event_handler(void)
{
    // extimate new glucose level
    srand(time(NULL) * node_id);

    int new_glu = glucose;
    int random = rand() % 8; // generate 0, 1, 2, 3, 4, 5, 6, 7

    if (random <3) {// 35% of changing the value
        if (random == 0) // decrease
            new_glu -= VARIATION;
        else // increase
            new_glu += VARIATION;
    }

    // if not equal
    if (new_glu != glucose)
    {
        glucose = new_glu;
        coap_notify_observers(&res_glucose);
    }
}