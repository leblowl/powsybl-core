/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.parameters;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.ModuleConfigUtil;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Parameter {

    private final List<String> names = new ArrayList<>();

    private final ParameterType type;

    private final String description;

    private final Object defaultValue;

    private final List<Object> possibleValues;

    public Parameter(String name, ParameterType type, String description, Object defaultValue,
                     List<Object> possibleValues) {
        names.add(Objects.requireNonNull(name));
        this.type = Objects.requireNonNull(type);
        this.description = Objects.requireNonNull(description);
        this.defaultValue = checkDefaultValue(type, defaultValue);
        this.possibleValues = checkPossibleValues(type, possibleValues, defaultValue);
    }

    public Parameter(String name, ParameterType type, String description, Object defaultValue) {
        this(name, type, description, defaultValue, null);
    }

    private static void checkValue(Class<?> typeClass, Object value) {
        if (value != null && !typeClass.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Bad default value type " + value.getClass() + ", " + typeClass + " was expected");
        }
    }

    private static void checkPossibleValuesContainsValue(List<Object> possibleValues, Object value,
                                                         Function<Object, IllegalArgumentException> exceptionCreator) {
        Objects.requireNonNull(possibleValues);
        Objects.requireNonNull(exceptionCreator);
        if (value != null) {
            if (value instanceof List) {
                for (Object valueElement : (List<?>) value) {
                    if (!possibleValues.contains(valueElement)) {
                        throw exceptionCreator.apply(valueElement);
                    }
                }
            } else {
                if (!possibleValues.contains(value)) {
                    throw exceptionCreator.apply(value);
                }
            }
        }
    }

    private static List<Object> checkPossibleValues(ParameterType type, List<Object> possibleValues, Object defaultValue) {
        if (possibleValues != null) {
            possibleValues.forEach(value -> checkValue(type.getElementClass(), value));
            checkPossibleValuesContainsValue(possibleValues, defaultValue, v -> {
                throw new IllegalArgumentException("Parameter possible values " + possibleValues + " should contain default value " + v);
            });
        }
        return possibleValues;
    }

    private static Object checkDefaultValue(ParameterType type, Object defaultValue) {
        checkValue(type.getTypeClass(), defaultValue);
        if (type == ParameterType.BOOLEAN && defaultValue == null) {
            throw new PowsyblException("With Boolean parameter you are not allowed to pass a null default value");
        }
        if (type == ParameterType.DOUBLE && defaultValue == null) {
            throw new PowsyblException("With Double parameter you are not allowed to pass a null default value");
        }
        return defaultValue;
    }

    public static Object read(String format, Properties parameters, Parameter configuredParameter, ParameterDefaultValueConfig defaultValueConfig) {
        Objects.requireNonNull(configuredParameter);
        switch (configuredParameter.getType()) {
            case BOOLEAN:
                return readBoolean(format, parameters, configuredParameter, defaultValueConfig);
            case STRING:
                return readString(format, parameters, configuredParameter, defaultValueConfig);
            case STRING_LIST:
                return readStringList(format, parameters, configuredParameter, defaultValueConfig);
            case DOUBLE:
                return readDouble(format, parameters, configuredParameter, defaultValueConfig);
            default:
                throw new IllegalStateException("Unknown parameter type: " + configuredParameter.getType());
        }
    }

    public static Object read(String format, Properties paramaters, Parameter configuredParameter) {
        return read(format, paramaters, configuredParameter, ParameterDefaultValueConfig.INSTANCE);
    }

    public static boolean readBoolean(String format, Properties parameters, Parameter configuredParameter, ParameterDefaultValueConfig defaultValueConfig) {
        return read(format, parameters, configuredParameter, defaultValueConfig.getBooleanValue(format, configuredParameter), ModuleConfigUtil::getOptionalBooleanProperty);
    }

    public static boolean readBoolean(String format, Properties parameters, Parameter configuredParameter) {
        return readBoolean(format, parameters, configuredParameter, ParameterDefaultValueConfig.INSTANCE);
    }

    public static String readString(String format, Properties parameters, Parameter configuredParameter, ParameterDefaultValueConfig defaultValueConfig) {
        return read(format, parameters, configuredParameter, defaultValueConfig.getStringValue(format, configuredParameter), ModuleConfigUtil::getOptionalStringProperty);
    }

    public static String readString(String format, Properties parameters, Parameter configuredParameter) {
        return readString(format, parameters, configuredParameter, ParameterDefaultValueConfig.INSTANCE);
    }

    public static List<String> readStringList(String format, Properties parameters, Parameter configuredParameter, ParameterDefaultValueConfig defaultValueConfig) {
        return read(format, parameters, configuredParameter, defaultValueConfig.getStringListValue(format, configuredParameter), ModuleConfigUtil::getOptionalStringListProperty);
    }

    public static List<String> readStringList(String format, Properties parameters, Parameter configuredParameter) {
        return readStringList(format, parameters, configuredParameter, ParameterDefaultValueConfig.INSTANCE);
    }

    public static double readDouble(String format, Properties parameters, Parameter configuredParameter, ParameterDefaultValueConfig defaultValueConfig) {
        return read(format, parameters, configuredParameter, defaultValueConfig.getDoubleValue(format, configuredParameter),
            (moduleConfig, names) -> ModuleConfigUtil.getOptionalDoubleProperty(moduleConfig, names).orElse(Double.NaN), value -> !Double.isNaN(value));
    }

    public static double readDouble(String format, Properties parameters, Parameter configuredParameter) {
        return readDouble(format, parameters, configuredParameter, ParameterDefaultValueConfig.INSTANCE);
    }

    private static <T> T read(String format, Properties parameters, Parameter configuredParameter, T defaultValue,
                              BiFunction<ModuleConfig, List<String>, T> supplier, Predicate<T> isPresent) {
        Objects.requireNonNull(format);
        Objects.requireNonNull(configuredParameter);
        T value = null;
        // priority on passed parameters
        if (parameters != null) {
            MapModuleConfig moduleConfig = new MapModuleConfig(parameters);
            value = supplier.apply(moduleConfig, configuredParameter.getNames());

            // check that if possible values are configured, value is contained in possible values
            if (value != null
                    && configuredParameter.getPossibleValues() != null) {
                checkPossibleValuesContainsValue(configuredParameter.getPossibleValues(), value,
                    v -> new IllegalArgumentException("Value " + v + " of parameter " + configuredParameter.getName() +
                                                      " is not contained in possible values " + configuredParameter.getPossibleValues()));
            }
        }
        // if none, use configured parameters
        if (isPresent.test(value)) {
            return value;
        }
        return defaultValue;
    }

    private static <T> T read(String format, Properties parameters, Parameter configuredParameter, T defaultValue, BiFunction<ModuleConfig, List<String>, Optional<T>> supplier) {
        return read(format, parameters, configuredParameter, defaultValue, (moduleConfig, strings) -> supplier.apply(moduleConfig, strings).orElse(null), Objects::nonNull);
    }

    public Parameter addAdditionalNames(String... names) {
        Objects.requireNonNull(names);
        this.names.addAll(Arrays.asList(names));
        return this;
    }

    public String getName() {
        return names.get(0);
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    public ParameterType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public List<Object> getPossibleValues() {
        return possibleValues;
    }
}
