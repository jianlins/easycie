package edu.utah.bmi.nlp.easycie.entry;


import edu.utah.bmi.nlp.core.SettingAb;

/**
 * The Setting class represents a setting with a name, value, description, double click action, and open click action.
 * It implements the SettingAb interface.
 */
public class Setting implements SettingAb {
    protected StrProperty settingName;


    protected StrProperty settingValue;
    protected StrProperty settingDesc;
    protected StrProperty doubleClick;
    protected StrProperty openClick;

    public Setting(String settingName, String settingValue, String settingDesc, String doubleClick) {
        init(settingName, settingValue, settingDesc, doubleClick, "");
    }

    public Setting(String settingName, String settingValue, String settingDesc, String doubleClick, String openClick) {
        init(settingName, settingValue, settingDesc, doubleClick, openClick);
    }

    public void init(String settingName, String settingValue, String settingDesc, String doubleClick, String openClick) {
        this.settingName = new StrProperty(settingName);
        this.settingValue = new StrProperty(settingValue);
        this.settingDesc = new StrProperty(settingDesc);
        this.doubleClick = new StrProperty(doubleClick);
        this.openClick = new StrProperty(openClick);
    }

    public String getSettingName() {
        return this.settingName.get();
    }

    public String getSettingValue() {
        return this.settingValue.get();
    }

    public String getSettingDesc() {
        return this.settingDesc.get();
    }

    public String getDoubleClick() {
        return this.doubleClick.get();
    }

    public String getOpenClick() {
        return this.openClick.get();
    }


    public StrProperty settingNameProperty() {
        return this.settingName;
    }

    public StrProperty settingValueProperty() {
        return this.settingValue;
    }

    public void setSettingValue(String settingValue){
        this.settingValue=new StrProperty(settingValue);
    }

    public StrProperty settingDescProperty() {
        return this.settingDesc;
    }

    public StrProperty doubleClickProperty() {
        return this.doubleClick;
    }

    public StrProperty openClickProperty() {
        return this.openClick;
    }

    public String serialize() {
        return this.settingName.get() + "|" + this.settingValue.get() + "|" + this.settingDesc.get()
                + "|" + this.doubleClick.get() + "|" + this.openClick.get();
    }

    public String toString() {
        return getSettingName();
    }

    public void setSettingNameProperty(String settingName) {
        this.settingName = new StrProperty(settingName);
    }


}
