/* Generated By:JavaCC: Do not edit this line. Prism3To4Constants.java */
package parser;

/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface Prism3To4Constants
{

	/** End of File. */
	int EOF = 0;
	/** RegularExpression Id. */
	int WHITESPACE = 1;
	/** RegularExpression Id. */
	int COMMENT = 2;
	/** RegularExpression Id. */
	int BOOL = 3;
	/** RegularExpression Id. */
	int CONST = 4;
	/** RegularExpression Id. */
	int CEIL = 5;
	/** RegularExpression Id. */
	int CTMC = 6;
	/** RegularExpression Id. */
	int CUMUL = 7;
	/** RegularExpression Id. */
	int DOUBLE = 8;
	/** RegularExpression Id. */
	int DTMC = 9;
	/** RegularExpression Id. */
	int ENDINIT = 10;
	/** RegularExpression Id. */
	int ENDMODULE = 11;
	/** RegularExpression Id. */
	int ENDREWARDS = 12;
	/** RegularExpression Id. */
	int ENDSYSTEM = 13;
	/** RegularExpression Id. */
	int FALSE = 14;
	/** RegularExpression Id. */
	int FLOOR = 15;
	/** RegularExpression Id. */
	int FORMULA = 16;
	/** RegularExpression Id. */
	int FUNC = 17;
	/** RegularExpression Id. */
	int FUTURE = 18;
	/** RegularExpression Id. */
	int GLOBAL = 19;
	/** RegularExpression Id. */
	int GLOB = 20;
	/** RegularExpression Id. */
	int INIT = 21;
	/** RegularExpression Id. */
	int INST = 22;
	/** RegularExpression Id. */
	int INT = 23;
	/** RegularExpression Id. */
	int LABEL = 24;
	/** RegularExpression Id. */
	int MAX = 25;
	/** RegularExpression Id. */
	int MDP = 26;
	/** RegularExpression Id. */
	int MIN = 27;
	/** RegularExpression Id. */
	int MODULE = 28;
	/** RegularExpression Id. */
	int NEXT = 29;
	/** RegularExpression Id. */
	int NONDETERMINISTIC = 30;
	/** RegularExpression Id. */
	int PMAX = 31;
	/** RegularExpression Id. */
	int PMIN = 32;
	/** RegularExpression Id. */
	int P = 33;
	/** RegularExpression Id. */
	int PROBABILISTIC = 34;
	/** RegularExpression Id. */
	int PROB = 35;
	/** RegularExpression Id. */
	int RATE = 36;
	/** RegularExpression Id. */
	int REWARDS = 37;
	/** RegularExpression Id. */
	int RMAX = 38;
	/** RegularExpression Id. */
	int RMIN = 39;
	/** RegularExpression Id. */
	int R = 40;
	/** RegularExpression Id. */
	int S = 41;
	/** RegularExpression Id. */
	int STOCHASTIC = 42;
	/** RegularExpression Id. */
	int SYSTEM = 43;
	/** RegularExpression Id. */
	int TRUE = 44;
	/** RegularExpression Id. */
	int UNTIL = 45;
	/** RegularExpression Id. */
	int NOT = 46;
	/** RegularExpression Id. */
	int AND = 47;
	/** RegularExpression Id. */
	int OR = 48;
	/** RegularExpression Id. */
	int IMPLIES = 49;
	/** RegularExpression Id. */
	int RARROW = 50;
	/** RegularExpression Id. */
	int COLON = 51;
	/** RegularExpression Id. */
	int SEMICOLON = 52;
	/** RegularExpression Id. */
	int COMMA = 53;
	/** RegularExpression Id. */
	int DOTS = 54;
	/** RegularExpression Id. */
	int LPARENTH = 55;
	/** RegularExpression Id. */
	int RPARENTH = 56;
	/** RegularExpression Id. */
	int LBRACKET = 57;
	/** RegularExpression Id. */
	int RBRACKET = 58;
	/** RegularExpression Id. */
	int LBRACE = 59;
	/** RegularExpression Id. */
	int RBRACE = 60;
	/** RegularExpression Id. */
	int EQ = 61;
	/** RegularExpression Id. */
	int NE = 62;
	/** RegularExpression Id. */
	int LT = 63;
	/** RegularExpression Id. */
	int GT = 64;
	/** RegularExpression Id. */
	int LE = 65;
	/** RegularExpression Id. */
	int GE = 66;
	/** RegularExpression Id. */
	int PLUS = 67;
	/** RegularExpression Id. */
	int MINUS = 68;
	/** RegularExpression Id. */
	int TIMES = 69;
	/** RegularExpression Id. */
	int DIVIDE = 70;
	/** RegularExpression Id. */
	int PRIME = 71;
	/** RegularExpression Id. */
	int RENAME = 72;
	/** RegularExpression Id. */
	int QMARK = 73;
	/** RegularExpression Id. */
	int DQUOTE = 74;
	/** RegularExpression Id. */
	int REG_INT = 75;
	/** RegularExpression Id. */
	int REG_DOUBLE = 76;
	/** RegularExpression Id. */
	int REG_IDENTPRIME = 77;
	/** RegularExpression Id. */
	int REG_IDENT = 78;
	/** RegularExpression Id. */
	int PREPROC = 79;

	/** Lexical state. */
	int DEFAULT = 0;

	/** Literal token values. */
	String[] tokenImage = { "<EOF>", "<WHITESPACE>", "<COMMENT>", "\"bool\"", "\"const\"", "\"ceil\"", "\"ctmc\"", "\"C\"", "\"double\"", "\"dtmc\"",
			"\"endinit\"", "\"endmodule\"", "\"endrewards\"", "\"endsystem\"", "\"false\"", "\"floor\"", "\"formula\"", "\"func\"", "\"F\"", "\"global\"",
			"\"G\"", "\"init\"", "\"I\"", "\"int\"", "\"label\"", "\"max\"", "\"mdp\"", "\"min\"", "\"module\"", "\"X\"", "\"nondeterministic\"", "\"Pmax\"",
			"\"Pmin\"", "\"P\"", "\"probabilistic\"", "\"prob\"", "\"rate\"", "\"rewards\"", "\"Rmax\"", "\"Rmin\"", "\"R\"", "\"S\"", "\"stochastic\"",
			"\"system\"", "\"true\"", "\"U\"", "\"!\"", "\"&\"", "\"|\"", "\"=>\"", "\"->\"", "\":\"", "\";\"", "\",\"", "\"..\"", "\"(\"", "\")\"", "\"[\"",
			"\"]\"", "\"{\"", "\"}\"", "\"=\"", "\"!=\"", "\"<\"", "\">\"", "\"<=\"", "\">=\"", "\"+\"", "\"-\"", "\"*\"", "\"/\"", "\"\\\'\"", "\"<-\"",
			"\"?\"", "\"\\\"\"", "<REG_INT>", "<REG_DOUBLE>", "<REG_IDENTPRIME>", "<REG_IDENT>", "<PREPROC>", };

}
