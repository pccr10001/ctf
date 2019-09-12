package li.power.app.antitank;

import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.util.Arrays;

public class UnsignedBigInteger extends BigInteger {
    public UnsignedBigInteger(byte[] value) {
        super(value);
    }

    public UnsignedBigInteger(@NonNull String value) {
        super(value);
    }

    public UnsignedBigInteger(@NonNull String value, int radix) {
        super(value, radix);
    }

    public byte[] toUnsignedByteArray() {
        byte[] signedValue = this.toByteArray();
        if (signedValue[0] != 0x00) {
            throw new IllegalArgumentException("value must be a positive BigInteger");
        }
        return Arrays.copyOfRange(signedValue, 1, signedValue.length);
    }

    public static BigInteger fromUnsignedByteArray(byte[] value) {
        byte[] signedValue = new byte[value.length + 1];
        System.arraycopy(value, 0, signedValue, 1, value.length);
        return new BigInteger(signedValue);
    }
}
