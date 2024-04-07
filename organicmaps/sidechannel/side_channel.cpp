#include "sidechannel/side_channel.hpp"

#include <jni.h>
#include <android/log.h>
#include <string>
#include <time.h>

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "hello-libs::", __VA_ARGS__))

namespace sidechannel
{
    // ground truth pointers for circular linked list
    struct node* head = nullptr;
    struct node* segmentPtr_1 = nullptr;
    struct node* segmentPtr_2 = nullptr;
    struct node* tail = nullptr;

    struct node* currentNode = NULL;

    int divideNum(int g, int h) {
        int result;
        result = g / h;

        return result;
    }

    int multiplyNum(int e, int f) {
        int result;
        result = e * f;
        divideNum(e, f);
        return result;
    }

    int subNum(int c, int d) {
        int result;
        result = c - d;
        multiplyNum(c, d);
        return result;
    }

    int addNum(int a, int b) {
        int result;
        result = a + b;
        return result;
    }

    long getTimeStamp() {
        struct timespec ts;
        clock_gettime(CLOCK_MONOTONIC, &ts);
        return (ts.tv_sec * 1000000 + ts.tv_nsec / 1000);
    }

    void insertNode(struct node *nodeData, struct node **head, struct node **tail) {
        struct node *newNode = (struct node*) malloc(sizeof(struct node));
        newNode->systemTime = nodeData->systemTime;
        newNode->methodId = nodeData->methodId;
        newNode->next = *head;

        if (*head == NULL) {
            newNode->next = newNode;
            *head = newNode;
            *tail = newNode;
        }

        if (*head != NULL) {
            (*tail)->next = newNode;
            *tail = newNode;
            (*tail)->next = *head;
        }
    }

    void initCircularLinkedList(node **curNode, node **hd, node **segPtr_1, node **segPtr_2, node **tl, int CONFIGURED_BUFFER_SIZE) {
        struct node node = {0,0,0};

        int circularBufSize = CONFIGURED_BUFFER_SIZE;
        for (int i = 0; i < circularBufSize; i++) {
            insertNode(&node, &(*hd), &(*tl));
        }
        LOGD("circularBufSize = %d", circularBufSize);

        // Initialize segment pointers
        struct node *nodePtr = *hd;
        int currentNodeIndex = 0;
        while (currentNodeIndex < circularBufSize) {
            if (currentNodeIndex == (circularBufSize / 3)) {
                *segPtr_1 = nodePtr;
                LOGD("[Ptr_1][%p] currentNodeIndex = %d", segPtr_1, currentNodeIndex);
            }
            if (currentNodeIndex == (2 * circularBufSize) / 3) {
                *segPtr_2 = nodePtr;
                LOGD("[Ptr_2][%p] currentNodeIndex = %d", segPtr_2, currentNodeIndex);
            }
            nodePtr = nodePtr->next;
            currentNodeIndex++;
        }
        *curNode = *hd;
    }

    //initCircularLinkedList(&currentNodeGT, &headGT, &segmentPtrGT_1, &segmentPtrGT_2, &tailGT, CONFIGURED_BUFFER_SIZE);

    void logMethodCall(int methodId) {
        timespec ts;
        clock_gettime(CLOCK_MONOTONIC, &ts);
        currentNode->systemTime = ts.tv_sec * 1000000 + ts.tv_nsec / 1000;
        currentNode->methodId = methodId;
        currentNode = currentNode->next;
        //__android_log_print(ANDROID_LOG_INFO, "GROUND_TRUTH_DEBUG", "%s", methodName);
    }

    extern "C" JNIEXPORT void JNICALL
    Java_app_organicmaps_MwmApplication_initCircularLinkedList(JNIEnv *, jclass, jint CONFIGURED_BUFFER_SIZE) {
        initCircularLinkedList(&currentNode, &head, &segmentPtr_1, &segmentPtr_2, &tail, CONFIGURED_BUFFER_SIZE);
    }

}   // namespace sidechannel