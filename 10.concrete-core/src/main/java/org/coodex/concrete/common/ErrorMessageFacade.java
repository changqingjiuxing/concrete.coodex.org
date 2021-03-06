package org.coodex.concrete.common;

import org.coodex.concrete.api.ErrorMsg;
import org.coodex.concrete.core.JavaTextFormatMessageFormatter;
import org.coodex.concrete.core.ResourceBundlesMessagePatternLoader;
import org.coodex.util.Common;
import org.coodex.util.SPIFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by davidoff shen on 2016-09-04.
 */
public class ErrorMessageFacade {

    /*
     * 没有MessageFormatter Provider的时候，以此Formatter输出，确保formatter不为空
     * 2016-11-01，修改默认使用JavaTextFormatMessageFormatter
     */
    private final static MessageFormatter DEFAULT_MESSAGE_FORMATTER = new JavaTextFormatMessageFormatter();

    private final static Logger log = LoggerFactory.getLogger(ErrorMessageFacade.class);

    private final static MessagePatternLoader DEFAULT_PATTERN_LOADER = new ResourceBundlesMessagePatternLoader();


    private static MessageFormatter getFormatter(Class<? extends MessageFormatter> formatterClass) {

        MessageFormatter formatter = formatterClass == null || formatterClass == MessageFormatter.class ?
                MESSAGE_FORMATTER_SPI_FACADE.getInstance() :
                MESSAGE_FORMATTER_SPI_FACADE.getInstance(formatterClass);

        return formatter == null ? DEFAULT_MESSAGE_FORMATTER : formatter;
    }

    private static MessagePatternLoader getPatternLoader(Class<? extends MessagePatternLoader> loaderClass) {
        MessagePatternLoader patternLoader = loaderClass == null || loaderClass == MessagePatternLoader.class ?
                MESSAGE_PATTERN_LOADER_SPI_FACADE.getInstance() :
                MESSAGE_PATTERN_LOADER_SPI_FACADE.getInstance(loaderClass);

        return patternLoader == null ? DEFAULT_PATTERN_LOADER : patternLoader;
    }


    private final static Map<Integer, Field> errorCodes = new HashMap<Integer, Field>();


    private static void registerClass(Class<? extends AbstractErrorCodes> clz) {
        if (clz == null) return;
        synchronized (errorCodes) {
            for (Field f : clz.getDeclaredFields()) {

                if (!(int.class.equals(f.getType())
                        && Modifier.isStatic(f.getModifiers())
                        && Modifier.isPublic(f.getModifiers()))) continue;

                f.setAccessible(true);
                try {
                    int code = f.getInt(null);
                    if (errorCodes.containsKey(code)) {
                        Field field = errorCodes.get(code);
                        if (field != f) {
                            log.warn("errorCode duplicate {}.{} and {}.{}",
                                    field.getDeclaringClass().getCanonicalName(), field.getName(),
                                    f.getDeclaringClass().getCanonicalName(), f.getName());
                        }
                    } else {
                        errorCodes.put(code, f);
                    }
                } catch (IllegalAccessException e) {
                    log.warn("Cannot bind errorCode: {}.{}",
                            f.getDeclaringClass().getCanonicalName(), f.getName());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void register(Class<? extends AbstractErrorCodes>... classes) {
        for (Class<? extends AbstractErrorCodes> clz : classes)
            registerClass(clz);
    }

    public static Set<Integer> allRegisteredErrorCodes() {
        return errorCodes.keySet();
    }

    public static String getMessageTemplate(int code) {
        return getMessageOrPattern(false, code);
    }

    public static String getMessage(int code, Object... objects) {
        return getMessageOrPattern(true, code, objects);
    }

    private static String getMessageOrPattern(boolean format, int code, Object... objects) {
        Field f = errorCodes.get(code);
        if (f == null) {
            log.debug("errorCode [{}] has not registed.", code);
            return null;
        }

        ErrorMsg errorMsg = f.getAnnotation(ErrorMsg.class);
        ErrorMsg formatterValue = errorMsg == null ? f.getDeclaringClass().getAnnotation(ErrorMsg.class) : errorMsg;

        MessageFormatter formatter = getFormatter(formatterValue == null ? null : formatterValue.formatterClass());

        String msgTemp = (errorMsg == null || Common.isBlank(errorMsg.value().trim())) ?
                "{message." + code + "}" : errorMsg.value();

        String pattern = msgTemp;
        if (msgTemp.startsWith("{") && msgTemp.endsWith("}")) {
            String key = msgTemp.substring(1, msgTemp.length() - 1).trim();
            pattern = getPatternLoader(errorMsg == null ? null : errorMsg.patternLoaderClass())
                    .getMessageTemplate(key);
        }

        return (pattern != null) ? (format ? formatter.format(pattern, objects) : pattern) : null;
    }

    private ErrorMessageFacade() {
    }

    private final static SPIFacade<MessageFormatter> MESSAGE_FORMATTER_SPI_FACADE = new ConcreteSPIFacade<MessageFormatter>() {
        @Override
        protected MessageFormatter getDefaultProvider() {
            return DEFAULT_MESSAGE_FORMATTER;
        }
    };

    private final static SPIFacade<MessagePatternLoader> MESSAGE_PATTERN_LOADER_SPI_FACADE = new ConcreteSPIFacade<MessagePatternLoader>() {
        @Override
        protected MessagePatternLoader getDefaultProvider() {
            return DEFAULT_PATTERN_LOADER;
        }
    };

}
