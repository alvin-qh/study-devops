package alvin.docker.common;

import alvin.docker.common.i18n.I18n;

import javax.inject.Provider;

public class I18nProvider implements Provider<I18n> {

    private final Context context;

    public I18nProvider(Context context) {
        this.context = context;
    }

    @Override
    public I18n get() {
        return Context.isAvailable() ? context.getI18n() : null;
    }
}
