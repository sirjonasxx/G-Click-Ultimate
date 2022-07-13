package extension;

import extension.game.FloorState;
import extension.game.FurniDataTools;
import extension.listeners.EnabledListener;
import extension.tools.*;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.misc.listenerpattern.Observable;
import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.util.function.Consumer;

@ExtensionInfo(
        Title =  "G-Click Ultimate",
        Description =  "Ultimate clicking tools",
        Version =  "0.1.2",
        Author =  "sirjonasxx"
)
public class GClickUltimateController extends ExtensionForm {

    // general
    public CheckBox chkAlwaysOnTop;
    public CheckBox enableGclickCbx;
    public Pane gclickPane;

    //click recharge
    public Button clickRechargeBtn;
    public AnchorPane rechargeStatePane;
    public Label rechargeLbl;

    //click through
    public RadioButton ctDisabled_rdb;
    public RadioButton ctEnabled_rdb;
    public RadioButton ctAlways_rdb;
    public CheckBox ctBlockSideloads_cbx;
    public CheckBox ctShiftNoWalk_cbx;

    //click schedule
    public CheckBox csScheduleClicks_cbx;
    public CheckBox csBurstClicks_cbx;
    public Spinner<Integer> burst_click_spinner;

    //others
    public CheckBox blockShiftRotations_cbx;
    public CheckBox autoShiftDoubleClick_cbx;
    public CheckBox antiAfkCbx; // no walking packets to prevent ratelimit disturbance

    //effect tools
    public RadioButton ceNone_rdb;
    public RadioButton ceDuck_rdb;
    public RadioButton ceGhost_rdb;
    public RadioButton ceBot_rdb;
    public RadioButton ceMini_rdb;
    public CheckBox ceKeep_cbx;


    private volatile FurniDataTools furniDataTools = null;
    private volatile FloorState floorState = null;

    private volatile ToggleFurniUtils toggleFurniUtils;
//    private volatile ClickScheduleTools clickScheduleTools;
    private volatile ClickThroughTools clickThroughTools;
    private volatile OtherClickTools otherClickTools;
    private volatile EffectTools effectTools;
    private volatile EnvironmentUtils environmentUtils;
    private volatile ShiftUtils shiftUtils;

    private volatile Observable<EnabledListener> enabledObservable = new Observable<>();
    private volatile Observable<InvalidationListener> disconnectObservable = new Observable<>();


    @Override
    protected void initExtension() {
        this.floorState = new FloorState(this);
        onConnect((host, i, s1, s2, hClient) -> furniDataTools = new FurniDataTools(host));
        floorState.requestRoom(this);

        // javafx spinner updates bugfix
        Spinner[] spinners = {burst_click_spinner};
        for(Spinner spinner : spinners) {
            spinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) spinner.increment(0); // won't change value, but will commit editor
            });
        }

        toggleFurniUtils = new ToggleFurniUtils(this);
//        clickScheduleTools = new ClickScheduleTools(this);
        clickThroughTools = new ClickThroughTools(this);
        otherClickTools = new OtherClickTools(this);
        effectTools = new EffectTools(this);
        environmentUtils = new EnvironmentUtils(this);
        shiftUtils = new ShiftUtils();
    }


    @Override
    protected void onEndConnection() {
        disconnectObservable.fireEvent(invalidationListener -> invalidationListener.invalidated(null));
    }

    public EnvironmentUtils getEnvironmentUtils() {
        return environmentUtils;
    }

    public ShiftUtils getShiftUtils() {
        return shiftUtils;
    }

    public ToggleFurniUtils getToggleFurniUtils() {
        return toggleFurniUtils;
    }

    public FloorState getFloorState() {
        return floorState;
    }

    public FurniDataTools getFurniDataTools() {
        return furniDataTools;
    }

    public Observable<EnabledListener> getEnabledObservable() {
        return enabledObservable;
    }

    public Observable<InvalidationListener> getDisconnectObservable() {
        return disconnectObservable;
    }

    public void burstCbxToggle(ActionEvent actionEvent) {
        burst_click_spinner.setDisable(!csBurstClicks_cbx.isSelected());
    }

    public void enableClick(ActionEvent actionEvent) {
        gclickPane.setDisable(!enableGclickCbx.isSelected());
        enabledObservable.fireEvent(enableGclickCbx.isSelected() ? EnabledListener::enabled : EnabledListener::disabled);
    }

    public void alwaysOnTopClick(ActionEvent actionEvent) {
        primaryStage.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
    }

    public boolean isEnabled() {
        return enableGclickCbx.isSelected();
    }
}
