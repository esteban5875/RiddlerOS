.PHONY: env compile run clean distclean

env:
	@$(MAKE) -C prototype env

compile: env
	@$(MAKE) -C prototype compile

run: compile
	@$(MAKE) -C prototype run

clean:
	@$(MAKE) -C prototype clean

distclean: clean
	@$(MAKE) -C prototype distclean
