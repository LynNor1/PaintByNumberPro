# PaintByNumberPro
## Purpose
The purpose of this program is to provide an interactive or automatic means of solving Nonograms, with emphasis on the ability to make and erase guesses easily.  Nonograms area also called Paint By Number, Griddlers, Hanjie, PicCross or Pic-a-Pix puzzles. 

#paintbynumber #griddler #hanjie #piccross #picapix #interactive #autosolver
## History
I love solving Nonograms.  But I have difficulty solving larger ones on paper and have never found a good way to manage making guesses and erasing the consequences of those guesses.  In 2010 I started writing this program in Java to give me a means of solving Nonograms that supported making guesses and testing the consequences of those guesses.  That naturally led to trying to write an automatic puzzle solver.  The code I wrote at that time got me part of the way there, but was particularly difficult to debug.  So I left it at a point where it would partially solve a Nonogram and then start using guesses to complete the puzzle.  This approach was very slow and didn't always lead to a solution in a reasonable amount of time.

In 2021, I decided to write a program that would help me extract the Nonogram clues from a scanned Nonogram.  That program is PaintByNumber_OCR which is in its own repository.  Now with new, larger puzzles to work with, I put this puzzle solver to the test.  I was quickly reminded that my old solver was less than stellar and I proceeded to rewrite some of the puzzle solving code.  The new code is much better than the old, but now we are left with a mix of my old and new code which may be a bit confusing to read.  But the end product is a fairly decent automatic solver that relies a little less on guessing.

Of note, there is one last shortcoming with the auto-solver.  I have noted it in the "Issues" and may get around to fixing it in the future.

For now, I hope you enjoy using this program to solve Nonograms.

## Code
This program was written entirely in Java 8 using NetBeans 8.2 on an Intel Mac running OS X 11.6 and does not require any additional libraries.  You can run this program on other platforms that support Java.  And you could work with the code using other IDEs.

## Getting Started

The PaintByNumberPro class contains the main function to run.  You can either run this program through your IDE or your can double-click on the PaintByNumberPro.jar file in your `dist` folder.  The following window will appear:
![opening window](readme_images/OpeningWindow.png)
If you click on `Open Puzzle...`, you will be selecting a .pbn file (really a text file) that contains a Nonogram or you can click on `Enter New Puzzle...` to create a new .pbn file by typing in all of the row and column clues.

You can also create a .pbn file by scanning or taking a photo of a Nonogram and using the **PaintByNumber_OCR** program to help you extract the clues.  The **PaintByNumber_OCR** program is in my other repository. 

### .pbn File Format
The format of a .pbn file is simple. It looks like this:

```
Source	Nonogram Book Puzzle 1
Rows	25
Cols	30
Row_clues	0	1	27
Row_clues	1	3	7	14	3
Row_clues	2	5	6	1	1	11	3
Row_clues	3	7	1	4	1	1	9	2	3
...
Row_clues	24	5	3	1	2	4	2
Col_clues	0	1	1
Col_clues	1	4	13	1	1	3
Col_clues	2	4	18	1	1	1
Col_clues	3	6	3	1	2	6	3	1
Col_clues	4	4	4	1	2	4
...
Col_clues	29	2	18	6
```

(The ... indicates that there are lines not shown). The first line contains the Source of the puzzle and is contains arbitrary text. The second and third lines tell you how many rows and columns there are in the puzzle. These are followed by the row clues and then the column clues. The 1st digit after Row_clues or Col_clues is the row or column number. It is followed by the number of clues for that row or column. And lastly come the clue values. Please keep these items in the order shown as the **PaintByNumberPro** program that reads the file is not particularly flexible.

Because this format is a text file, you can always create one by hand in any text editor.

### Creating a .pbn File Using OCR

You can create a .pbn file using OCR (Optical Character Recognition) using the **PaintByNumber_OCR** program in my other repository.  Using this program requires a little more setup because it uses Tesseract OCR and OpenCV.

### Creating a .pbn File

Click on the `Enter New Puzzle...` button:
![enter new puzzle](readme_images/EnterNewPuzzle.png)
Give your puzzle a name and enter the number of columns and rows.  Press `OK`.
![enter puzzle clues](readme_images/EnterPuzzleClues.png)
This dialog gives you a means of typing in the clues (separated by spaces) in the middle text field.  Use the radio buttons to choose between row and column clues.  Move to the previous and next set of clues using the `<-` and `->` buttons.  When you have entered all of your clues, hit `Done`.  The program will do some basic error checking and will then give you the opportunity to save the new .pbn file.

This part of the code could be made a little more smart, but was provided just for convenience.

### Loading a .pbn File

Click on the `Open Puzzle...` button in the main window.  Choose your .pbn file.  The program will read and load the puzzle file.  If there are any errors in the .pbn file, you will be notified with a a dialog message.

## MainWindow
The following two windows appear after you have successfully loaded a .pbn file:
![main windows](readme_images/PuzzleAndMainWindow.png)
The particular puzzle shown was a freebie from Conceptis Puzzles.

You can use the puzzle window to interactively solve the puzzle (more instructions on this later).  The window on the left is used to control the appearance of the puzzle and gives you many options for saving, checking, and solving the puzzle using automated means.

### Controlling the Puzzle Appearance