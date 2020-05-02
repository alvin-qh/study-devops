package alvin.docker.core;

import alvin.docker.core.web.I18n;

import javax.inject.Provider;

public class I18nProvider implements Provider<I18n> {

    private final Context context;

    public I18nProvider(Context context) {
        this.context = context;
    }

    @Override
    public I18n get() {
        return context.getI18n();
    }
}
