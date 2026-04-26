package liu1.liu11.liu1.liu1.plugin.form;

import java.util.EventObject;

import kd.bos.base.AbstractBasePlugIn;
import kd.bos.form.control.Control;

public class BaseDataFormPlugin extends AbstractBasePlugIn {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addClickListeners("btnsave");
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);

        Object source = evt.getSource();
        if (source instanceof Control && "btnsave".equals(((Control) source).getKey())) {
            this.getView().showTipNotification("你已经点击了保存");
        }
    }
}
