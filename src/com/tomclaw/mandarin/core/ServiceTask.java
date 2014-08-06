package com.tomclaw.mandarin.core;

import com.tomclaw.mandarin.main.ChiefActivity;

/**
 * Created by Solkin on 08.07.2014.
 */
public abstract class ServiceTask extends WeakObjectTask<ChiefActivity>
        implements ChiefActivity.CoreServiceListener {

    public ServiceTask(ChiefActivity object) {
        super(object);
    }

    @Override
    public final void executeBackground() throws Throwable {
        ChiefActivity activity = getWeakObject();
        if (activity != null) {
            ServiceInteraction interaction = activity.getServiceInteraction();
            activity.removeCoreServiceListener(this);
            if (interaction == null || !activity.isCoreServiceReady()) {
                activity.addCoreServiceListener(this);
                onServiceRestarting();
            } else {
                executeServiceTask(interaction);
            }
        }
    }

    public abstract void executeServiceTask(ServiceInteraction interaction) throws Throwable;

    public void onServiceRestarting() {
    }

    @Override
    public final void onCoreServiceReady() {
        // Retry execute itself.
        TaskExecutor.getInstance().execute(this);
    }

    @Override
    public final void onCoreServiceDown() {
    }
}
