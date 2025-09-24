package com.bound4.image.application.port.out;

import com.bound4.image.domain.FileHash;

public interface HashService {
    FileHash calculateHash(byte[] data);
}