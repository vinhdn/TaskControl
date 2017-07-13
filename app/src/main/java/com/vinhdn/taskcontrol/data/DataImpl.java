package com.vinhdn.taskcontrol.data;

import java.util.Date;

/**
 * Created by vinh on 7/13/17.
 */

public abstract class DataImpl implements Data{

    public DataImpl(){
        this.timeCollected = new Date().toGMTString();
    }

    protected String timeCollected = new Date().toGMTString();

    @Override
    public String toData() {
        return "collected at " + timeCollected;
    }
}
