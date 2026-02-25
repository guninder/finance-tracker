import { useState, useEffect, useCallback } from 'react';
import API from '../api/axios';
import { toast } from 'react-toastify';
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis,
  CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import {
  TrendingUp, TrendingDown, DollarSign, Plus, Pencil, Trash2,
} from 'lucide-react';
import TransactionModal from '../components/TransactionModal';
import LoadingSpinner from '../components/LoadingSpinner';

const CHART_COLORS = [
  '#6366f1', '#f59e0b', '#10b981', '#ef4444',
  '#8b5cf6', '#ec4899', '#14b8a6',
];

export default function Dashboard() {
  const [summary, setSummary] = useState({ totalIncome: 0, totalExpenses: 0, balance: 0 });
  const [transactions, setTransactions] = useState([]);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [loadingTransactions, setLoadingTransactions] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [modalKey, setModalKey] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  const fetchSummary = useCallback(async () => {
    setLoadingSummary(true);
    try {
      const response = await API.get('/summary');
      setSummary(response.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to load summary');
    } finally {
      setLoadingSummary(false);
    }
  }, []);

  const fetchTransactions = useCallback(async () => {
    setLoadingTransactions(true);
    try {
      const response = await API.get('/transactions');
      setTransactions(response.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to load transactions');
    } finally {
      setLoadingTransactions(false);
    }
  }, []);

  useEffect(() => {
    fetchSummary();
    fetchTransactions();
  }, [fetchSummary, fetchTransactions]);

  const handleAddTransaction = () => {
    setEditingTransaction(null);
    setModalKey((k) => k + 1);
    setModalOpen(true);
  };

  const handleEditTransaction = (transaction) => {
    setEditingTransaction(transaction);
    setModalKey((k) => k + 1);
    setModalOpen(true);
  };

  const handleDeleteTransaction = async (id) => {
    if (!window.confirm('Are you sure you want to delete this transaction?')) return;
    try {
      await API.delete(`/transactions/${id}`);
      toast.success('Transaction deleted');
      fetchTransactions();
      fetchSummary();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete transaction');
    }
  };

  const handleModalSubmit = async (formData) => {
    setSubmitting(true);
    try {
      if (editingTransaction) {
        await API.put(`/transactions/${editingTransaction.id}`, formData);
        toast.success('Transaction updated');
      } else {
        await API.post('/transactions', formData);
        toast.success('Transaction added');
      }
      setModalOpen(false);
      setEditingTransaction(null);
      fetchTransactions();
      fetchSummary();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save transaction');
    } finally {
      setSubmitting(false);
    }
  };

  const categoryData = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((acc, t) => {
      const existing = acc.find((item) => item.name === t.category);
      if (existing) {
        existing.value += parseFloat(t.amount);
      } else {
        acc.push({ name: t.category, value: parseFloat(t.amount) });
      }
      return acc;
    }, []);

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(val);
  };

  return (
    <div className="dashboard">
      {/* Summary Cards */}
      <div className="summary-cards">
        <div className="summary-card income-card">
          <div className="summary-card-icon">
            <TrendingUp size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-label">Total Income</p>
            {loadingSummary ? (
              <LoadingSpinner size={20} />
            ) : (
              <h3 className="summary-value">{formatCurrency(summary.totalIncome)}</h3>
            )}
          </div>
        </div>

        <div className="summary-card expense-card">
          <div className="summary-card-icon">
            <TrendingDown size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-label">Total Expenses</p>
            {loadingSummary ? (
              <LoadingSpinner size={20} />
            ) : (
              <h3 className="summary-value">{formatCurrency(summary.totalExpenses)}</h3>
            )}
          </div>
        </div>

        <div className="summary-card balance-card">
          <div className="summary-card-icon">
            <DollarSign size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-label">Balance</p>
            {loadingSummary ? (
              <LoadingSpinner size={20} />
            ) : (
              <h3 className="summary-value">{formatCurrency(summary.balance)}</h3>
            )}
          </div>
        </div>
      </div>

      {/* Chart Section */}
      <div className="chart-section">
        <div className="chart-card">
          <h3 className="chart-title">Spending by Category</h3>
          {loadingTransactions ? (
            <LoadingSpinner />
          ) : categoryData.length === 0 ? (
            <p className="chart-empty">No expense data to display</p>
          ) : (
            <div className="chart-container">
              <div className="chart-pie">
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={categoryData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) =>
                        `${name} ${(percent * 100).toFixed(0)}%`
                      }
                      outerRadius={100}
                      dataKey="value"
                    >
                      {categoryData.map((entry, index) => (
                        <Cell
                          key={`cell-${entry.name}`}
                          fill={CHART_COLORS[index % CHART_COLORS.length]}
                        />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => formatCurrency(value)} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div className="chart-bar">
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={categoryData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip formatter={(value) => formatCurrency(value)} />
                    <Legend />
                    <Bar dataKey="value" name="Amount" fill="#6366f1" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Transactions Table */}
      <div className="transactions-section">
        <div className="transactions-header">
          <h3>Recent Transactions</h3>
          <button className="btn btn-primary" onClick={handleAddTransaction}>
            <Plus size={18} />
            <span>Add Transaction</span>
          </button>
        </div>

        {loadingTransactions ? (
          <LoadingSpinner />
        ) : transactions.length === 0 ? (
          <div className="transactions-empty">
            <p>No transactions yet. Click &quot;Add Transaction&quot; to get started.</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="transactions-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Type</th>
                  <th>Description</th>
                  <th>Amount</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((t) => (
                  <tr key={t.id} className={t.type === 'INCOME' ? 'row-income' : 'row-expense'}>
                    <td>{t.date}</td>
                    <td>
                      <span className="category-badge">{t.category}</span>
                    </td>
                    <td>
                      <span className={`type-badge ${t.type === 'INCOME' ? 'type-income' : 'type-expense'}`}>
                        {t.type}
                      </span>
                    </td>
                    <td className="description-cell">{t.description || '—'}</td>
                    <td className={t.type === 'INCOME' ? 'amount-income' : 'amount-expense'}>
                      {t.type === 'INCOME' ? '+' : '-'}{formatCurrency(t.amount)}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button
                          className="btn-icon btn-edit"
                          onClick={() => handleEditTransaction(t)}
                          title="Edit"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          className="btn-icon btn-delete"
                          onClick={() => handleDeleteTransaction(t.id)}
                          title="Delete"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <TransactionModal
        key={modalKey}
        isOpen={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setEditingTransaction(null);
        }}
        onSubmit={handleModalSubmit}
        transaction={editingTransaction}
        loading={submitting}
      />
    </div>
  );
}
