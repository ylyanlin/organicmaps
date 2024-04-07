//
// Created by giraffe on 4/4/22.
//

#ifndef ANDROID_CIRCULARLINKEDLIST_H
#define ANDROID_CIRCULARLINKEDLIST_H

#include <logoutput.h>

struct node* head = NULL;
struct node *segmentPtr_1 = NULL;
struct node *segmentPtr_2 = NULL;
struct node* tail = NULL;
int circularBufSize = 0;

struct node {
    long systemTime;
    long count;
    int scanTime;
    long address;
    struct node *next;
};

void insertNode(struct node *nodeData) {
    struct node *newNode = (struct node*) malloc(sizeof(struct node));
    newNode->systemTime = nodeData->systemTime;
    newNode->count = nodeData->count;
    newNode->scanTime = nodeData->scanTime;
    newNode->address = nodeData->address;
    newNode->next = head;

    if (head == NULL) {
        newNode->next = newNode;
        head = newNode;
        tail = newNode;
    }

    if (head != NULL) {
        tail->next = newNode;
        tail = newNode;
        tail->next = head;
    }
}

void display() {
    struct node *ptr;
    ptr = head;

    if (head != NULL) {
        do {
            LOGD("address: %d scanTime: %d", ptr->address, ptr->scanTime);
            ptr = ptr->next;
        } while (ptr != head);
    }
}

#endif //ANDROID_CIRCULARLINKEDLIST_H
