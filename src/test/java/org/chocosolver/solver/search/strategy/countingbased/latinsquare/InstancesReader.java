package org.chocosolver.solver.search.strategy.countingbased.latinsquare;
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

//	public static MagicSquare readMagicSquareInstance(String filepath) {
//		FileReader fr;
//		int n = 9;
//		int[][] instances = new int[n][n];
//		try {
//			fr = new FileReader(filepath);
//			try {
//				char[] line = new char[5];
//				fr.read(line);
//				int nbFilled = Integer.parseInt("" + line[2] + line[3]);
//				for (int k = 0; k < nbFilled; k++) {
//					line = new char[3];
//					fr.read(line);
//					int i = Integer.parseInt(line[0]+"")-1;
//					int j = Integer.parseInt(line[2]+"")-1;
//					fr.read();
//					char c1 = (char) fr.read();
//					char c2 = (char) fr.read();
//					int number = (c2 != '\n' ? Integer.parseInt("" + c1 + c2) : Integer.parseInt("" + c1));
//					if (c2 != '\n') {
//						fr.read();
//					}
//					instances[i][j] = number;
//				}
//				fr.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}		
//		return new MagicSquare(instances);
//	}

	public static void main(String[] args) {
		String filepath = "instances/LatinSquare/qwh.o30.h374.12.pls";
		LatinSquare ls = readLatinSquareInstance(filepath);
		ls.setCBSStrategy("");
		//ls.setDOWDStrategy("");
		ls.solve();

	}

}
