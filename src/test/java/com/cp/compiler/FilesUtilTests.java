package com.cp.compiler;

import com.cp.compiler.utility.FilesUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ExtendWith(SpringExtension.class)
public class FilesUtilTests {
	
	@TempDir
	Path tempDir;
	
	@Test
	public void shouldDeleteFile() throws IOException {
		// Given
		Path path = tempDir.resolve("test.txt");
		Files.write(path, "test".getBytes());
		
		// When
		FilesUtil.deleteFile(path.getParent().toString(), "test.txt");
		
		// Then
		Assertions.assertFalse(Files.exists(path));
	}
	
	@Test
	public void shouldReturnTrueAfterFileDeletion() throws IOException {
		// Given
		Path path = tempDir.resolve("test.txt");
		Files.write(path, "test".getBytes());
		
		// When
		boolean isDeleted = FilesUtil.deleteFile(path.getParent().toString(), "test.txt");
		
		// Then
		Assertions.assertTrue(isDeleted);
	}
	
	@Test
	public void shouldReturnFalseIfFileIsNull() throws IOException {
		// Given
		Path path = tempDir.resolve("test.txt");
		Files.write(path, "test".getBytes());
		
		// When
		boolean isDeleted = FilesUtil.deleteFile(path.getParent().toString(), null);
		
		// Then
		Assertions.assertFalse(isDeleted);
	}
	
	@Test
	public void shouldReturnFalseIfFolderIsNull() throws IOException {
		// Given
		Path path = tempDir.resolve("test.txt");
		Files.write(path, "test".getBytes());
		
		// When
		boolean isDeleted = FilesUtil.deleteFile(null, "test.txt");
		
		// Then
		Assertions.assertFalse(isDeleted);
	}
}
