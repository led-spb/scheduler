package ru.led.scheduler.tools;

public class CircularBuffer<T> {
    private int capacity;
    private int head, tail;
    private T[] buffer;
    
    @SuppressWarnings("unchecked")
    public CircularBuffer(int capacity) {
	this.capacity = capacity;
	buffer = (T[]) new Object[capacity];
	head = 0;
	tail = 0;
    }
    
    public synchronized boolean isEmpty(){
	return head==tail;
    }

    public synchronized void put(T data){
        buffer[head] = data;

        if( ++head>= capacity ) head=0;
        if( head==tail )
            if( ++tail>=capacity ) tail=0;
    }
    
    public synchronized T get() throws IllegalArgumentException{
        if( isEmpty() )
            throw new IllegalArgumentException("Buffer is empty");
        T result = buffer[tail];
        if( ++tail >= capacity ) tail=0;
        return result;
    }
    
    public synchronized int count(){
	return head>=tail? (head-tail): (head-tail+capacity);
    }
}
