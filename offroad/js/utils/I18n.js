import { NativeModules } from 'react-native'
import { setupI18n } from "@lingui/core"
import zhTW from '../locales/zh_TW/messages.js';

// define which locale use which translate file
let supportedLanguage = {
    'zh_TW': zhTW,
};

const locale = NativeModules.I18nManager.localeIdentifier; // zh_TW_#Hant, zh_CN_#Hans

export const i18n = setupI18n();
i18n.load(supportedLanguage);
// if the locale is defined, activate it
if (supportedLanguage.hasOwnProperty(locale)) {
    i18n.activate(locale);
}

export default i18n