package com.panpan.maimiaoautoconfigure.config;

/**
 * @author xiaobo
 * @description
 * @date 2023/2/22 10:15
 */
public class MaimiaoException extends RuntimeException {
    private String msg;

    public MaimiaoException(String msg) {
        super(msg);
        this.msg = msg;
    }
}
