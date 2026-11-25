
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;



public class Main {


	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		String choiseStr;
		String sourceFile, resultFile, firstFile, secondFile;
		
		loop: while (true) {
			
			choiseStr = sc.next();
								
			switch (choiseStr) {
			case "comp":
				System.out.print("source file name: ");
				sourceFile = sc.next();
				System.out.print("archive name: ");
				resultFile = sc.next();
				comp(sourceFile, resultFile);
				break;
			case "decomp":
				System.out.print("archive name: ");
				sourceFile = sc.next();
				System.out.print("file name: ");
				resultFile = sc.next();

				decomp(sourceFile, resultFile);
				break;
			case "size":
				System.out.print("file name: ");
				sourceFile = sc.next();
				size(sourceFile);
				break;
			case "equal":
				System.out.print("first file name: ");
				firstFile = sc.next();
				System.out.print("second file name: ");
				secondFile = sc.next();
				System.out.println(equal(firstFile, secondFile));
				break;
			case "about":
				about();
				break;
			case "exit":
				break loop;
            
            default:
                System.out.println("wrong command");

			}
		}

		sc.close();
	}

	public static void comp(String sourceFile, String resultFile) {
    // Linda un Samanta
        try {
            // nolasa failiu baitos un izveido baitu masīvu
            FileInputStream fis = new FileInputStream(sourceFile);
            byte[] data = fis.readAllBytes();
            fis.close();

            // izveido vārdnīcu un aizpilda lidz 256 (jo ir 256 iespejami viena baita simboli)
            Map<List<Byte>, Integer> vardnica = new HashMap<>();
            for (int i = 0; i < 256; i++) {
                vardnica.put(List.of((byte) i), i); // i parveido par baitu un pieskir tam i vertibu
            }
            //tagad katrs iespejamais viens baits jau ir vardnica

            int vardnicasIzm = 256; // viena baita simbolu vardnicas izmers
            int maxVardnica = 65536; // 2^16, un ar 16 bitiem var attēlot 65536 dažādas vērtības
        

            // implemente LZW algoritmu
            List<Byte> simboli = new ArrayList<>(); // simbolu saraksts
            List<Integer> saspiestais = new ArrayList<>(); // parveidotais lists ko liksim kompresetaja faila

            for (byte b : data) { // iet cauri katram baitam (pa vienam) no baitu masiva
                List<Byte> jauniSimboli = new ArrayList<>(simboli); // izveido sarakstu kur liks JAUNUS simbolus, krc skatās arī nākamo baitu un tad meklē vai pastāv jau tāda kombinācija
                jauniSimboli.add(b);

                if (vardnica.containsKey(jauniSimboli)) {
                    simboli = jauniSimboli; // tatad kombinacija jau ir vardnica, var ņemt nākamo baitu un skatīties vai ar to ir jau vēl garāka kombinācija, TAPEC LIEKAM SIMBOLOS
                } else {
                    saspiestais.add(vardnica.get(simboli)); // aizvieto jauno kombināciju ar skaitlisku kodu
                    
                    if (vardnicasIzm < maxVardnica) {
                        vardnica.put(jauniSimboli, vardnicasIzm++); // ieliek vardnīcā jauno secību un piešķir kodu
                    }

                    simboli = new ArrayList<>(); // iztukšo simbolu list, lai var viedot jaunas kombinācijas
                    simboli.add(b);
                }
            }

            if (!simboli.isEmpty()) {
                saspiestais.add(vardnica.get(simboli)); // ieliek pēdējo baitu saspiestajā list
            }
        
            // Write codes into binary file (2 bytes each)
            FileOutputStream out = new FileOutputStream(resultFile); // atver failu rakstīšanai
            int bitBuffer = 0;//pagaidu mainigais prms mes ierakstam faila
            int bitCount = 0;// cik pagaidu bitu ir buferī
            int bits = 16;
            maxVardnica = 1 << bits;//vardnicas max izmers

            DataOutputStream dout = new DataOutputStream(out);//lai rakstītu bināros datus ļoti primitīvus
            dout.writeInt(saspiestais.size());//vajag decomp daļā, lai zinātu cik daudz kodu ir failā

            for (int c : saspiestais) {//iet cauri katram kodam no saspiestā lista
                bitBuffer = (bitBuffer << bits) | (c & ((1 << bits) -1 ));//pārvieto bitu buferi pa kreisi par 16 un pievieno jauno kodu
                bitCount = bitCount + bits;

                while(bitCount >= 8) {//kamēr ir pietiekami daudz bitu lai izveidotu vienu baitu
                    int oneByte = (bitBuffer >> (bitCount - 8)) & 0xFF;//pabīda vajadzīgos 8 bitus uz labo pusi un paņem tieši vienu baitu
                    dout.write(oneByte);//ieraksta tikai 1 baitu un samazina bitu skaitu buferī par 8
                    bitCount = bitCount -8;
                }
            }
            if (bitCount > 0) {//ja ir palikuši kādi biti buferī, tad ieraksta tos failā (piebīdot pa kreisi)
                    int pedejaisBaits = (bitBuffer << (8 - bitCount)) & 0xFF;//pabīda atlikušos bitus uz kreiso pusi lai aizpildītu vienu baitu
                    dout.write(pedejaisBaits);//ieraksta pēdējo baitu failā
            }

            dout.close();

            System.out.println("OK");

        } catch (Exception e) {
            System.out.println("Error in comp: " + e.getMessage());
    }
    }

    public static void decomp(String sourceFile, String resultFile) {
        // Agate un Signe
        try {
            // Atver compresēto failu, lai izlasītu bināros datus
            DataInputStream in = new DataInputStream(new FileInputStream(sourceFile));

            int bits = 16;
            int maxDict = 1 << bits;

            // Izlasa cik daudz kodi bija uzrakstīti
            int codeCount = in.readInt();
            // Ja nav kodu, tad izveido tukšu output failu un apstājas
            if (codeCount <= 0) {
                new FileOutputStream(resultFile).close();
                in.close();
                return;
            }

            // Izlasa bit-packed kodus (13 biti katrs) izmantojot operacijas
            List<Integer> codes = new ArrayList<>(codeCount); // Uzglabā izvilkto LZW kodu
            int bitBuffer = 0; // Īslaicījā bitu storage
            int bitCount = 0; // Tas cik biti tiek uzkrāti pašlaik

            while (codes.size() < codeCount) { //Turpinās lidz viss kods ir izvilkts
                int nextByte = in.read(); // Izlasa tikai vietu baitu (8 bitus) no faila
                if (nextByte == -1) break; // Pārstāj, ja ir sasniegta faila beigas

                // Pastumj vecos bitus pa kreisi pa 8 pozīcijām, pievieno jaunu baitu bufferim
                bitBuffer = (bitBuffer << 8) | (nextByte & 0xFF);
                bitCount += 8; //Pievieno 8 jaunus bitus

                //Ja ir pietiekami daudz bitu lai izveidotu kodu, to extracto
                while (bitCount >= bits && codes.size() < codeCount) {
                    //Pārvieto 13 augšējos bitus pa labi, nomaskē visu izņemot tos 13, iznākums ir viens LZW kods
                    int code = (bitBuffer >> (bitCount - bits)) & ((1 << bits) - 1);
                    codes.add(code); //Uzglabā kodu
                    bitCount -= bits; // Izņem nelietotos bitus no buffera
                }
            }
            in.close();

            if (codes.isEmpty()) {
                new FileOutputStream(resultFile).close();
                return;
            }

            // LZW decompression
            Map<Integer, List<Byte>> dict = new HashMap<>();
            for (int i = 0; i < 256; i++) {
                dict.put(i, List.of((byte) i));
            }

            int dictSize = 256; // Nākošais pieejamais vārdnīcas indeks

            // Paņem pirmo kodu, pārveido uz baitu sekvenci, saglabā to kā w
            List<Byte> w = new ArrayList<>(dict.get(codes.remove(0)));
            // Inicializē output ar pirmo sekvenci
            List<Byte> result = new ArrayList<>(w);

            for (int code : codes) { // Noprocesse atlikušo kodu
                List<Byte> entry; // Uzglabā dekodēto baitu sekvenci

                if (dict.containsKey(code)) { // Standarta notikums - ir jau vārdnīcā notikums
                    entry = dict.get(code);
                } else if (code == dictSize) { // Īpašs LZW notikums
                    entry = new ArrayList<>(w);
                    entry.add(w.get(0));
                } else { // 'Corrupted' vai 'Invalid' dati
                    throw new IOException("Invalid LZW code: " + code);
                }

                result.addAll(entry); // Appendo dekotētos baitus uz output

                // Iveido jaunu vārdnīcas entry
                if (dictSize < maxDict) {
                    List<Byte> newEntry = new ArrayList<>(w);
                    newEntry.add(entry.get(0));
                    dict.put(dictSize++, newEntry);
                }

                w = entry; //Sagatavo nakošajai iterācijai
            }

            // Write bytes back to file
            FileOutputStream fos = new FileOutputStream(resultFile); //Atver output faili
            // Konvertē List<Byte> uz byte[]
            byte[] arr = new byte[result.size()];
            for (int i = 0; i < result.size(); i++) arr[i] = result.get(i);
            //Ieraksta dekompresijas baitus failā
            fos.write(arr);
            fos.close();

            System.out.println("OK"); // Gatavs :D

        } catch (Exception e) {
            System.out.println("Error in decomp: " + e.getMessage());
        }
    }

    public static void size(String sourceFile) {
        try {
            FileInputStream f = new FileInputStream(sourceFile);
            System.out.println("size: " + f.available());
            f.close();
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }

	public static boolean equal(String firstFile, String secondFile) {
		try {
			FileInputStream f1 = new FileInputStream(firstFile);
			FileInputStream f2 = new FileInputStream(secondFile);
			int k1, k2;
			byte[] buf1 = new byte[1000];
			byte[] buf2 = new byte[1000];
			do {
				k1 = f1.read(buf1);
				k2 = f2.read(buf2);
				if (k1 != k2) {
					f1.close();
					f2.close();
					return false;
				}
				for (int i=0; i<k1; i++) {
					if (buf1[i] != buf2[i]) {
						f1.close();
						f2.close();
						return false;
					}
						
				}
			} while (!(k1 == -1 && k2 == -1));
			f1.close();
			f2.close();
			return true;
		}
		catch (IOException ex) {
			System.out.println(ex.getMessage());
			return false;
		}
	}
	
	public static void about() {
		System.out.println("251RDB046 Linda Faituša, 15");
		System.out.println("251RDB137 Signe Zvirbule, 9");
		System.out.println("251RDB071 Agate Fridmane, 6");
		System.out.println("251RDB034 Marta Spriņģe, 15");
		System.out.println("251RDB021 Samanta Kelmere, 15");
	}
}
