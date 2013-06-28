package thc.sandbox.marketmanager.recorder

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

class FileRecorder(file: File) {
	if(file.exists) file.delete
	file.createNewFile
	
	//exceptions thrown on first write? i have no idea what lazy does lol
	private lazy val writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))
	
	def writeLine(line: String) = writer.println(line)
	def flush() = writer.flush()
	def close() = writer.close()
}