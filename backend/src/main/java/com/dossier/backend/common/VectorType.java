package com.dossier.backend.common;

import com.pgvector.PGvector;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Hibernate UserType that maps float[] to PostgreSQL vector type via pgvector.
 * Avoids calling PGvector.toArray() to prevent compile-time dependency on
 * org.postgresql.util.PGBinaryObject (postgresql driver is runtime scope).
 */
public class VectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position,
                               SharedSessionContractImplementor session,
                               Object owner) throws SQLException {
        Object obj = rs.getObject(position);
        if (obj == null) return null;
        // Parse vector string format: [0.1,0.2,...] or (0.1,0.2,...)
        return parseVectorString(obj.toString());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            // PGvector(float[]) constructs the vector object; Types.OTHER lets the driver handle it
            st.setObject(index, new PGvector(value), Types.OTHER);
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) throws HibernateException {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) throws HibernateException {
        return deepCopy((float[]) cached);
    }

    private static float[] parseVectorString(String s) {
        s = s.trim();
        if (s.startsWith("[") || s.startsWith("(")) {
            s = s.substring(1, s.length() - 1);
        }
        String[] parts = s.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
