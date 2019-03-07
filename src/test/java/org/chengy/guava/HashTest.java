package org.chengy.guava;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class HashTest {


    /**
     * 校验总和hash函数
     */
    public void hashFunctionTest() {
        HashFunction adler32 = Hashing.adler32();
        HashFunction crc32 = Hashing.crc32();

        HashCode hashCode = adler32.hashBytes(new byte[4]);

    }

	public static void main(String[] args) {

		Thread.currentThread().interrupt();

		System.out.println("interrupt");
	}
}
