package ai.prophetizo.demo;

import ai.prophetizo.wavelet.cwt.finance.*;
import ai.prophetizo.wavelet.cwt.finance.FinancialWaveletAnalyzer.*;

import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Interactive live trading simulation using streaming analyzers.
 * 
 * <p>This demo simulates a trading bot that uses wavelet analysis to make
 * real-time trading decisions with portfolio tracking.</p>
 */
public class LiveTradingSimulation {
    
    // ANSI colors
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    
    // Trading portfolio
    private static class Portfolio {
        private double cash = 10_000.0; // Starting capital
        private double shares = 0;
        private double entryPrice = 0;
        private final List<Trade> trades = new ArrayList<>();
        private double peakValue = cash;
        
        public void buy(double price, double confidence) {
            if (cash > 0 && shares == 0) {
                shares = cash / price;
                entryPrice = price;
                cash = 0;
                trades.add(new Trade(TradeType.BUY, price, shares, confidence));
            }
        }
        
        public void sell(double price, double confidence) {
            if (shares > 0) {
                cash = shares * price;
                double profit = (price - entryPrice) * shares;
                trades.add(new Trade(TradeType.SELL, price, shares, confidence, profit));
                shares = 0;
            }
        }
        
        public double getValue(double currentPrice) {
            return cash + shares * currentPrice;
        }
        
        public double getReturn(double currentPrice) {
            return (getValue(currentPrice) - 10_000.0) / 10_000.0;
        }
        
        public void updatePeak(double currentPrice) {
            double value = getValue(currentPrice);
            if (value > peakValue) {
                peakValue = value;
            }
        }
        
        public double getDrawdown(double currentPrice) {
            double value = getValue(currentPrice);
            return peakValue > 0 ? (peakValue - value) / peakValue : 0;
        }
        
        public int getWinningTrades() {
            return (int) trades.stream()
                .filter(t -> t.type == TradeType.SELL && t.profit > 0)
                .count();
        }
        
        public int getLosingTrades() {
            return (int) trades.stream()
                .filter(t -> t.type == TradeType.SELL && t.profit <= 0)
                .count();
        }
    }
    
    private enum TradeType { BUY, SELL }
    
    private record Trade(
        TradeType type,
        double price,
        double shares,
        double confidence,
        double profit
    ) {
        Trade(TradeType type, double price, double shares, double confidence) {
            this(type, price, shares, confidence, 0);
        }
    }
    
    // Market event simulator
    private static class MarketEventSimulator {
        private final Random random = new Random(42);
        private double basePrice = 100.0;
        private double drift = 0.0001;
        private double volatility = 0.02;
        private int time = 0;
        
        // Market events
        private final List<MarketEvent> events = Arrays.asList(
            new MarketEvent(100, 150, "📈 Earnings beat expectations", 0.05, 0.01),
            new MarketEvent(300, 320, "📉 Fed raises interest rates", -0.03, 0.03),
            new MarketEvent(500, 510, "💥 Market flash crash", -0.10, 0.05),
            new MarketEvent(700, 750, "🚀 Product launch success", 0.08, 0.02),
            new MarketEvent(900, 920, "⚡ High volatility period", 0, 0.04)
        );
        
        record MarketEvent(
            int startTime,
            int endTime,
            String description,
            double priceImpact,
            double volatilityImpact
        ) {}
        
        public MarketTick next() {
            time++;
            
            // Check for events
            String event = null;
            for (MarketEvent e : events) {
                if (time == e.startTime) {
                    event = e.description;
                    basePrice *= (1 + e.priceImpact);
                    volatility = e.volatilityImpact;
                } else if (time > e.startTime && time <= e.endTime) {
                    volatility = e.volatilityImpact;
                } else if (time == e.endTime + 1) {
                    volatility = 0.02; // Return to normal
                }
            }
            
            // Generate price
            double randomWalk = random.nextGaussian() * volatility;
            double trend = drift * time;
            double seasonality = Math.sin(time * 0.01) * 0.01;
            
            basePrice *= (1 + randomWalk);
            double price = basePrice * (1 + trend + seasonality);
            
            // Generate volume (higher during events)
            double baseVolume = 1_000_000;
            double volumeMultiplier = volatility > 0.02 ? 2.0 : 1.0;
            double volume = baseVolume * volumeMultiplier * (1 + random.nextDouble() * 0.2);
            
            return new MarketTick(time, price, volume, event, volatility);
        }
    }
    
    private record MarketTick(
        int timestamp,
        double price,
        double volume,
        String event,
        double marketVolatility
    ) {}
    
    public static void main(String[] args) throws Exception {
        System.out.println(BOLD + CYAN + "=== VectorWave Live Trading Simulation ===" + RESET);
        System.out.println("Starting with $10,000 capital...\n");
        Thread.sleep(1000);
        
        // Initialize components
        Portfolio portfolio = new Portfolio();
        IncrementalFinancialAnalyzer analyzer = new IncrementalFinancialAnalyzer(
            FinancialAnalysisParameters.builder()
                .crashAsymmetryThreshold(8.0)  // More sensitive
                .regimeTrendThreshold(0.02)
                .build(),
            100,  // window size
            5     // update interval
        );
        
        MarketEventSimulator market = new MarketEventSimulator();
        
        // Trading statistics
        int totalSignals = 0;
        double maxDrawdown = 0;
        
        // Main trading loop
        for (int i = 0; i < 1000; i++) {
            MarketTick tick = market.next();
            
            // Update portfolio metrics
            portfolio.updatePeak(tick.price);
            maxDrawdown = Math.max(maxDrawdown, portfolio.getDrawdown(tick.price));
            
            // Analyze market
            var analysis = analyzer.processSample(tick.price, tick.volume);
            
            // Clear screen for live update
            if (i % 5 == 0) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
                
                // Print header
                printHeader(tick, portfolio);
                
                // Print market analysis
                printMarketAnalysis(analysis, tick);
                
                // Handle trading signals
                if (analysis.hasSignal()) {
                    totalSignals++;
                    handleSignal(analysis, portfolio, tick.price);
                }
                
                // Print portfolio status
                printPortfolioStatus(portfolio, tick.price, maxDrawdown);
                
                // Print recent trades
                printRecentTrades(portfolio);
                
                // Show event if any
                if (tick.event != null) {
                    System.out.println("\n" + YELLOW + BOLD + "📰 BREAKING: " + 
                        tick.event + RESET);
                }
            }
            
            // Simulate real-time
            Thread.sleep(50);
        }
        
        // Final summary
        printFinalSummary(portfolio, market.basePrice, totalSignals, maxDrawdown);
    }
    
    private static void printHeader(MarketTick tick, Portfolio portfolio) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        System.out.printf(BOLD + CYAN + "VectorWave Trading Bot " + RESET + 
            DIM + "[%s]" + RESET + "\n", timestamp);
        System.out.println("─".repeat(70));
    }
    
    private static void printMarketAnalysis(
            IncrementalFinancialAnalyzer.IncrementalAnalysisResult analysis,
            MarketTick tick) {
        
        // Price and trend
        String priceColor = analysis.return_() > 0 ? GREEN : RED;
        String trendArrow = analysis.return_() > 0 ? "↑" : "↓";
        
        System.out.printf("\n" + BOLD + "Market Analysis" + RESET + "\n");
        System.out.printf("Price: %s$%.2f %s %.2f%%%s  Volume: %.0f\n",
            priceColor + BOLD, analysis.price(), trendArrow, 
            Math.abs(analysis.return_() * 100), RESET, tick.volume);
        
        // EMAs
        System.out.printf("EMAs: " + DIM + "12:" + RESET + " $%.2f  " +
            DIM + "26:" + RESET + " $%.2f  " +
            DIM + "50:" + RESET + " $%.2f\n",
            analysis.ema12(), analysis.ema26(), analysis.ema50());
        
        // Volatility and regime
        String volColor = analysis.volatility() > 0.03 ? RED : 
                         analysis.volatility() > 0.02 ? YELLOW : GREEN;
        
        System.out.printf("Volatility: %s%.2f%%%s  Regime: %s\n",
            volColor, analysis.volatility() * 100, RESET,
            formatRegime(analysis.regime()));
        
        // Risk meter
        System.out.printf("Risk Level: %s\n", formatRiskMeter(analysis.riskLevel()));
        
        // Crash warning
        if (analysis.crashDetected()) {
            System.out.println(RED + BOLD + "⚠️  CRASH WARNING! Market instability detected!" + RESET);
        }
    }
    
    private static void handleSignal(
            IncrementalFinancialAnalyzer.IncrementalAnalysisResult analysis,
            Portfolio portfolio,
            double price) {
        
        if (analysis.signal() == SignalType.BUY) {
            portfolio.buy(price, analysis.signalStrength());
            System.out.println("\n" + GREEN + BOLD + "📈 BUY SIGNAL" + RESET +
                String.format(" (%.0f%% confidence)", analysis.signalStrength() * 100));
        } else if (analysis.signal() == SignalType.SELL) {
            portfolio.sell(price, analysis.signalStrength());
            System.out.println("\n" + RED + BOLD + "📉 SELL SIGNAL" + RESET +
                String.format(" (%.0f%% confidence)", analysis.signalStrength() * 100));
        }
    }
    
    private static void printPortfolioStatus(Portfolio portfolio, double price, double maxDrawdown) {
        System.out.printf("\n" + BOLD + "Portfolio Status" + RESET + "\n");
        
        double value = portfolio.getValue(price);
        double returnPct = portfolio.getReturn(price) * 100;
        String returnColor = returnPct >= 0 ? GREEN : RED;
        
        System.out.printf("Value: $%.2f  %s%+.2f%%%s\n",
            value, returnColor + BOLD, returnPct, RESET);
        
        if (portfolio.shares > 0) {
            System.out.printf("Position: %.2f shares @ $%.2f\n",
                portfolio.shares, portfolio.entryPrice);
            double unrealized = (price - portfolio.entryPrice) * portfolio.shares;
            String unrealizedColor = unrealized >= 0 ? GREEN : RED;
            System.out.printf("Unrealized P&L: %s$%+.2f%s\n",
                unrealizedColor, unrealized, RESET);
        } else {
            System.out.println("Position: " + DIM + "Cash (no position)" + RESET);
        }
        
        System.out.printf("Max Drawdown: %.2f%%\n", maxDrawdown * 100);
    }
    
    private static void printRecentTrades(Portfolio portfolio) {
        System.out.printf("\n" + BOLD + "Recent Trades" + RESET + "\n");
        
        int start = Math.max(0, portfolio.trades.size() - 5);
        var recentTrades = portfolio.trades.subList(start, portfolio.trades.size());
        
        if (recentTrades.isEmpty()) {
            System.out.println(DIM + "No trades yet" + RESET);
        } else {
            for (Trade trade : recentTrades) {
                String icon = trade.type == TradeType.BUY ? "🟢" : "🔴";
                String profitStr = trade.type == TradeType.SELL ? 
                    String.format(" P&L: %s$%+.2f%s", 
                        trade.profit >= 0 ? GREEN : RED, trade.profit, RESET) : "";
                
                System.out.printf("%s %-4s @ $%.2f%s\n",
                    icon, trade.type, trade.price, profitStr);
            }
        }
        
        if (portfolio.trades.size() > 0) {
            int wins = portfolio.getWinningTrades();
            int losses = portfolio.getLosingTrades();
            double winRate = wins + losses > 0 ? (double) wins / (wins + losses) : 0;
            
            System.out.printf(DIM + "Win rate: %.0f%% (%d wins, %d losses)" + RESET + "\n",
                winRate * 100, wins, losses);
        }
    }
    
    private static void printFinalSummary(Portfolio portfolio, double finalPrice, 
                                        int totalSignals, double maxDrawdown) {
        System.out.println("\n\n" + BOLD + CYAN + "=== Trading Summary ===" + RESET);
        System.out.println("─".repeat(50));
        
        double finalReturn = portfolio.getReturn(finalPrice) * 100;
        String returnColor = finalReturn >= 0 ? GREEN : RED;
        
        System.out.printf("Starting Capital: $10,000\n");
        System.out.printf("Final Value: $%.2f\n", portfolio.getValue(finalPrice));
        System.out.printf("Total Return: %s%+.2f%%%s\n",
            returnColor + BOLD, finalReturn, RESET);
        System.out.printf("Max Drawdown: %.2f%%\n", maxDrawdown * 100);
        
        System.out.printf("\nTotal Trades: %d\n", portfolio.trades.size());
        System.out.printf("Winning Trades: %d\n", portfolio.getWinningTrades());
        System.out.printf("Losing Trades: %d\n", portfolio.getLosingTrades());
        System.out.printf("Total Signals: %d\n", totalSignals);
        
        // Calculate Sharpe ratio (simplified)
        double avgDailyReturn = finalReturn / 1000 / 100; // Convert to daily
        double sharpe = avgDailyReturn / 0.02 * Math.sqrt(252); // Annualized
        System.out.printf("Sharpe Ratio: %.2f\n", sharpe);
        
        System.out.println("\n" + GREEN + "✓ Simulation complete!" + RESET);
    }
    
    private static String formatRegime(MarketRegime regime) {
        return switch (regime) {
            case TRENDING_UP -> GREEN + "↑ Bull" + RESET;
            case TRENDING_DOWN -> RED + "↓ Bear" + RESET;
            case VOLATILE -> YELLOW + "⚡ Volatile" + RESET;
            case RANGING -> BLUE + "↔ Sideways" + RESET;
        };
    }
    
    private static String formatRiskMeter(double risk) {
        int level = (int) (risk * 5);
        String meter = "█".repeat(level) + "░".repeat(5 - level);
        
        String color = risk > 0.8 ? RED : risk > 0.5 ? YELLOW : GREEN;
        return color + meter + RESET + String.format(" %.0f%%", risk * 100);
    }
}