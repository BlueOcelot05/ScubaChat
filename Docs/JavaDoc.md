# JAVADOC CHEATSHEET

## Introduction

The major important thing is the documentation has to be implementation independent and specification concise.
Dependencies where ever necessary are allowed to be specified.  
Also it is allows HTML tags to be used in between the documentation comments. Pretty much all tags are self explanatory.

__Meta Annotations__  
_@author_&emsp;&emsp;__Ex:__ @author Jane Doe  
_@version_&emsp;&emsp;__Ex:__ @version v1.0-alpha  
_@since_&emsp;&emsp;__Ex:__ @since 2015-09-22

__Method Annotations__  
_@param&emsp;parameter&emsp;description_&emsp;&emsp;__Ex:__ @param&emsp;img&emsp;the image to be passed  
_@return&emsp;parameter&emsp;description_&emsp;&emsp;__Ex:__ @return&emsp;img&emsp;the image to be returned  
_@throws/exception&emsp;description_&emsp;&emsp;__Ex:__ @throws&emsp;IOException&emsp;If I/O exception occurred

__Deprecated__  
_@deprecated_&emsp;deprecated-text(_optional_)&emsp;&emsp;__Ex:__ @deprecated&emsp;since version v1.0

__Linking__  
_@see&emsp;reference_&emsp;&emsp;__Ex:__ @see package.ClassA/_url_/_string_  
_{@link&emsp;package.class#member&emsp;label}_&emsp;&emsp;__Ex:__ {@link URL}/{@link ImageObserver}&emsp;This is
descriptive text  
_@serial/serialField/serialData_&emsp;&emsp;__Ex:__ @serialField Field1

## Important Notes

1. Write keywords in __"code"__ tag example : __null__ with this tag.

2. Add the tags in the following order :  
   @author&emsp;(classes and interfaces)  
   @version&emsp;(classes and interfaces)  
   @param&emsp;(methods and constructors)  
   @return&emsp;(methods)  
   @exception&emsp;(@throws from Javadoc 1.2)  
   @see&emsp;(If multiple @see, then order according to distance from current file)  
   @since&emsp;(Follow format throughout document)  
   @serial&emsp;(or @serialField or @serialData)  
   @deprecated&emsp;(see How and When To Deprecate APIs)

## Basic Command Working Examples

1. javadoc AddAcc.java&emsp;:&emsp;Simply creates Javadoc file in the same directory.
2. javadoc -d doc BankAcc.java&emsp;:&emsp;creates Javadoc files for the specified file in same directory in specified
   folder __doc__.
3. javadoc -d doc bankpackage/*.java&emsp;:&emsp;Javadoc files for complete package created in __doc__ folder in current
   directory.
4. javadoc -tag newTag. : a : ”New Tag: “ -d doc BankAcc.java&emsp;:&emsp;For adding new custom tag in documentation for
   either specified file or package.







