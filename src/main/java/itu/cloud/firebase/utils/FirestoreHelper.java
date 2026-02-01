package itu.cloud.firebase.utils;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
public class FirestoreHelper {

    public static <T> T convert(DocumentSnapshot doc, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            Map<String, Object> data = doc.getData();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                assert data != null;
                Object value = data.get(fieldName);

                if (value == null) continue;

                try {
                    Object convertedValue = convertValue(value, field.getType());
                    if (convertedValue != null) {
                        field.set(instance, convertedValue);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur de conversion pour le champ " + fieldName + " : " + e.getMessage());
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'instance " + clazz.getName(), e);
        }
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;

        if (value instanceof Timestamp timestamp && targetType.equals(LocalDateTime.class)) {
            return LocalDateTime.ofInstant(timestamp.toDate().toInstant(), ZoneId.systemDefault());
        }

        if (value instanceof Number number) {
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                if (Double.isNaN(number.doubleValue())) return null;
                return number.intValue();
            }
            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                if (Double.isNaN(number.doubleValue())) return null;
                return number.longValue();
            }
            if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return number.doubleValue();
            }
            if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return number.floatValue();
            }
        }

        if (value instanceof Map && !targetType.equals(Map.class) && !targetType.isPrimitive() && !targetType.getName().startsWith("java.")) {
            return convertMapToObj((Map<String, Object>) value, targetType);
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        return null;
    }

    private static <T> T convertMapToObj(Map<String, Object> data, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = data.get(field.getName());
                if (value != null) {
                    field.set(instance, convertValue(value, field.getType()));
                }
            }
            return instance;
        } catch (Exception e) {
            return null;
        }
    }
}
