/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.sql;


import org.jasypt.util.text.BasicTextEncryptor;

/**
 * Created by u0876964 on 12/8/2016.
 */
public class Decrypt {
    public static String decrypt(String encryptedString) {
        String password;
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        password = Encrypt.getMasterPassword();
        textEncryptor.setPassword(password);
        password = textEncryptor.decrypt(encryptedString);
        return password;
    }
}
