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

import org.apache.commons.io.FileUtils;
import org.jasypt.util.text.BasicTextEncryptor;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Use jcrypt to encrypt your input string
 * Created by u0876964 on 12/8/2016.
 */
public class Encrypt {
    public static void main(String[] args) throws UnknownHostException, SocketException {
        String password;
        InetAddress ip = InetAddress.getLocalHost();

        String sb = getMasterPassword();

        System.out.println(sb);

        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(sb);

        if (args.length == 0) {
            System.out.print("Please enter your password (will be displayed in console): ");
            password = System.console().readLine();
        } else {
            password = args[0];
        }
        password = textEncryptor.encrypt(password);
        System.out.println("This is your encrypted password: \n" + password);
        System.out.println("Use (without quotes): \"ENC(" + password + ")\" to fill the password setting in your configuration file");

        String configFile = "";
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                configFile = args[i];
                updateConfigFile(password, configFile);
                System.out.println(new File(configFile).getAbsolutePath() + " has been updated");
            }
        }
    }

    private static void updateConfigFile(String password, String configFile) {
        File file = new File(configFile);
        if (file.exists()) {
            try {
                String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                content = content.replaceAll(">ENC\\([^\\)]+\\)<", ">ENC(" + password + ")<");
                FileUtils.write(file, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getMasterPassword() {
        StringBuilder output = new StringBuilder();
        InetAddress ip = null;
        try {
            ip = InetAddress.getLocalHost();

            output.append(ip.getHostName());
            output.append("  ");
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface currentNetwork = networks.nextElement();
//            output.setLength(0);
                byte[] mac = currentNetwork.getHardwareAddress();
                if (mac != null) {
//               Skip docker's network
                    if (currentNetwork.getName().startsWith("dock") || currentNetwork.getName().startsWith("veth"))
                        continue;
                    output.append(currentNetwork.getName());
                    output.append(" ");
                    for (int i = 0; i < mac.length; i++) {
                        output.append(String.format("%02X", mac[i]));
                    }
                    output.append("  ");
//                System.out.println(output.toString());
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return output.toString().trim();
    }
}
