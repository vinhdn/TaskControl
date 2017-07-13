package com.vinhdn.taskcontrol.data;

/**
 * Created by vinh on 7/13/17.
 */

public class BatteryInfo extends DataImpl{
    private int level;

    public BatteryInfo(int level) {
        super();
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String toData() {
        return "Battery level: " + level + " " + super.toData();
    }
}
