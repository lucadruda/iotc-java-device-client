package com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.models.Storage;

public interface ICentralStorage {
    void persist(Storage storage);
    Storage retrieve();
}
