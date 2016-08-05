package parser.ast;

import ltl.parser.Comparison;

public class ExpressionFrequencyG extends ExpressionTemporal
{
	public final double bound;
	public final Comparison cmpOperator; // the cmpOperator
	public final boolean isLimInf; //true if we are considering lim inf, else: false

	public ExpressionFrequencyG(double bound, Comparison cmpOperator, boolean isLimInf)
	{
		this.bound = bound;
		this.cmpOperator = cmpOperator;
		this.isLimInf = isLimInf;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o.getClass() != this.getClass()) {
			return false;
		}
		ExpressionFrequencyG freq = (ExpressionFrequencyG) o;
		return super.equals(o) && this.bound == freq.bound && this.cmpOperator == freq.cmpOperator && this.isLimInf == freq.isLimInf;
	}

	@Override
	public String toString()
	{
		String s = "G {";
		s = s + (isLimInf ? "inf" : "sup");
		s = s + " ";
		s = s + cmpOperator.toString();
		s = s + " ";
		s = s + bound;
		s = s + "} ";
		return s + super.toString().substring(1);
	}

	@Override
	public void setOperator(int op)
	{
		if (op == ExpressionTemporal.P_FREQ) {
			super.setOperator(op);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int getOperator()
	{
		return ExpressionTemporal.P_FREQ;
	}

	@Override
	public ExpressionFrequencyG deepCopy()
	{
		ExpressionFrequencyG expr = new ExpressionFrequencyG(this.bound, this.cmpOperator, this.isLimInf);
		expr.setOperator(op);
		if (operand1 != null)
			expr.setOperand1(operand1.deepCopy());
		if (operand2 != null)
			expr.setOperand2(operand2.deepCopy());
		expr.setLowerBound(lBound == null ? null : lBound.deepCopy(), lBoundStrict);
		expr.setUpperBound(uBound == null ? null : uBound.deepCopy(), uBoundStrict);
		expr.equals = equals;
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}
