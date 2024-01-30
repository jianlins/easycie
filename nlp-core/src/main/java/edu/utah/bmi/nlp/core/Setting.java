package edu.utah.bmi.nlp.core;



public class Setting implements SettingAb {
    protected String settingName;


    protected String settingValue;
    protected String settingDesc;
    protected String doubleClick;
    protected String openClick;

    public Setting(String settingName, String settingValue, String settingDesc, String doubleClick) {
        init(settingName, settingValue, settingDesc, doubleClick, "");
    }

    public Setting(String settingName, String settingValue, String settingDesc, String doubleClick, String openClick) {
        init(settingName, settingValue, settingDesc, doubleClick, openClick);
    }

    public void init(String settingName, String settingValue, String settingDesc, String doubleClick, String openClick) {
        this.settingName = settingName;
        this.settingValue = settingValue;
        this.settingDesc = settingDesc;
        this.doubleClick = doubleClick;
        this.openClick = openClick;
    }

    public String getSettingName() {
        return this.settingName;
    }

    public String getSettingValue() {
        return this.settingValue;
    }

    public String getSettingDesc() {
        return this.settingDesc;
    }

    public String getDoubleClick() {
        return this.doubleClick;
    }

    public String getOpenClick() {
        return this.openClick;
    }


    public String settingNameProperty() {
        return this.settingName;
    }

    public String settingValueProperty() {
        return this.settingValue;
    }

    public void setSettingValue(String settingValue){
        this.settingValue= settingValue;
    }

    public String settingDescProperty() {
        return this.settingDesc;
    }

    public String doubleClickProperty() {
        return this.doubleClick;
    }

    public String openClickProperty() {
        return this.openClick;
    }

    public String serialize() {
        return this.settingName + "|" + this.settingValue + "|" + this.settingDesc
                + "|" + this.doubleClick + "|" + this.openClick;
    }

    public String toString() {
        return getSettingName();
    }

    public void setSettingNameProperty(String settingName) {
        this.settingName = settingName;
    }


}
