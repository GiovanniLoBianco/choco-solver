/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.countingbased;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class InstancesReader {

	public static LatinSquare readLatinSquareInstance(String filepath) {
		FileReader fr;
		int n = 30;
		int[][] instances = new int[n][n];
		try {
			fr = new FileReader(filepath);
			try {
				char[] line = new char[9];
				fr.read(line);
				for (int i = 0; i < n; i++) {
					line = new char[4 * n + 1];
					fr.read(line);
					// la case à remplir dans instance[i]
					int j = 0;
					// le curseur sur la ligne
					int cursor = 0;
					String nb = "";
					while (cursor < 4 * n + 1) {
						if (line[cursor] == ' ' || line[cursor] == '\n') {
							if (nb != "") {
								instances[i][j] = Integer.parseInt(nb);
								j++;
							}
							nb = "";
						} else {
							nb += line[cursor];
						}
						cursor++;
					}
				}
				fr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return new LatinSquare(instances);
	}



}
