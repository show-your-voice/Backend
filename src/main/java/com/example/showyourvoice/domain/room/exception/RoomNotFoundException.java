package com.example.showyourvoice.domain.room.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class RoomNotFoundException extends BusinessException {

    public RoomNotFoundException() {
        super(ErrorCode.ROOM_NOT_FOUND, "roomId");
    }
}
