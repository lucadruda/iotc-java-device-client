package com.github.lucadruda.iotcclient;

import com.github.lucadruda.iotc.device.ICentralStorage;
import com.github.lucadruda.iotc.device.models.Storage;

import java.nio.charset.StandardCharsets;

public class MemoryStorage implements ICentralStorage {

    @Override
    public void persist(Storage storage) {
        System.out.println("New credentials available:");
        System.out.println(storage.getHubName());
        System.out.println(storage.getDeviceId());
        System.out.println(new String(storage.getDeviceKey(), StandardCharsets.UTF_8));
        return;
    }

    @Override
    public Storage retrieve() {
        return new Storage();
    }

}